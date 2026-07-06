package com.qdash.presentation.update

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.UpdateInfo
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.SavingsAmber
import java.io.File

@Composable
fun StatusHero(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun CheckingUpdatesSection(
    steps: List<CheckStepItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusHero(
            icon = Icons.Default.Sync,
            iconColor = MaterialTheme.colorScheme.primary,
            title = "جاري التحقق من التحديثات...",
            subtitle = "نحن نقوم الآن بالاتصال بالخادم للبحث عن إصدارات جديدة"
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                steps.forEach { step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (step.status) {
                            CheckStepStatus.PENDING -> {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(1.5.dp, TextGray.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                            CheckStepStatus.RUNNING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            CheckStepStatus.COMPLETED -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            CheckStepStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (step.status == CheckStepStatus.RUNNING) FontWeight.Bold else FontWeight.Normal,
                            color = if (step.status == CheckStepStatus.RUNNING) MaterialTheme.colorScheme.primary
                                    else if (step.status == CheckStepStatus.PENDING) TextGray.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateCard(
    info: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("إصدار جديد متوفر!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("الإصدار v${info.versionName}", style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("حجم الملف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    val sizeInMb = com.qdash.core.utils.FormatterUtils.convertNumerals(String.format("%.2f MB", info.apkSize.toDouble() / (1024 * 1024)))
                    Text(sizeInMb, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المصدر", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Text("GitHub Releases", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (!info.releaseNotes.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                Text("ما الجديد في هذا الإصدار:", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadingUpdatesSection(
    progress: Int,
    speed: String,
    eta: String,
    onPauseDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("جاري تحميل التحديث...", fontWeight = FontWeight.Bold)
                if (progress >= 0) {
                    Text("$progress%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (progress >= 0) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (speed.isNotEmpty() || (eta.isNotEmpty() && eta != "حساب...")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (speed.isNotEmpty()) {
                        Text(text = "السرعة: $speed", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                    if (eta.isNotEmpty() && eta != "حساب...") {
                        Text(text = "المتبقي: $eta", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onPauseDownload,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إيقاف مؤقت", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PausedUpdatesSection(
    progress: Int,
    onResumeDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("تم إيقاف التحميل مؤقتاً", fontWeight = FontWeight.Bold)
                Text("$progress%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onResumeDownload,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استئناف التحميل", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FallbackRecoveryFlow(
    viewModel: UpdatesViewModel,
    info: UpdateInfo,
    file: File,
    step: FallbackStep,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportManualBackup(uri)
        } else {
            Toast.makeText(context, "تم إلغاء التصدير.", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ExpenseRed.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                }
                Column {
                    Text("تعذر التثبيت التلقائي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    Text("معالجة تعارض التوقيع الرقمي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
            }

            Text(
                text = "يرفض نظام أندرويد تثبيت التحديث مباشرة فوق التطبيق الحالي. يحدث هذا عادة بسبب اختلاف مفتاح التوقيع الرقمي للمطور (Signature Mismatch). لحل المشكلة، يرجى اتباع الخطوات الإرشادية التالية لحماية بياناتك وتثبيت التحديث:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            // Step 1: Export Backup
            val step1Done = step != FallbackStep.BACKUP
            FallbackStepItem(
                number = 1,
                title = "تصدير نسخة احتياطية لبياناتك",
                desc = "هام جداً: سيؤدي إلغاء التثبيت إلى حذف قاعدة البيانات. اضغط لتصدير نسخة احتياطية كاملة وحفظها بأمان (في Drive أو التنزيلات).",
                isActive = step == FallbackStep.BACKUP,
                isDone = step1Done,
                actionButton = {
                    Button(
                        onClick = { backupLauncher.launch("Qdash_Backup.zip") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ البيانات الاحتياطية", fontSize = 12.sp)
                    }
                }
            )

            // Step 2: Copy APK
            val step2Done = step == FallbackStep.UNINSTALL
            FallbackStepItem(
                number = 2,
                title = "نسخ ملف التحديث إلى التنزيلات",
                desc = "حفظ ملف التثبيت APK باسم (Qdash-Install-This.apk) في مجلد التنزيلات بالجهاز لتتمكن من فتحه وتثبيته لاحقاً.",
                isActive = step == FallbackStep.COPY_APK,
                isDone = step2Done,
                actionButton = {
                    Button(
                        onClick = { viewModel.copyApkToDownloadsFolder(context, file, info) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نسخ ملف APK الآن", fontSize = 12.sp)
                    }
                }
            )

            // Step 3: Uninstall App
            FallbackStepItem(
                number = 3,
                title = "إزالة النسخة الحالية وتثبيت التحديث",
                desc = "اضغط لإلغاء تثبيت هذا التطبيق. بعد الإزالة، اذهب إلى تطبيق الملفات (Files/Downloads) في جهازك، وافتح Qdash-Install-This.apk لتثبيته. عند فتح التطبيق الجديد، استورد البيانات من النسخة الاحتياطية.",
                isActive = step == FallbackStep.UNINSTALL,
                isDone = false,
                actionButton = {
                    Button(
                        onClick = { viewModel.launchUninstall(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء التثبيت ومتابعة", fontSize = 12.sp)
                    }
                }
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            TextButton(
                onClick = { viewModel.cancelFallback(info, file) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إلغاء والعودة للخلف", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun FallbackStepItem(
    number: Int,
    title: String,
    desc: String,
    isActive: Boolean,
    isDone: Boolean,
    actionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (isActive || isDone) 1f else 0.5f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) IncomeGreen
                    else if (isActive) MaterialTheme.colorScheme.primary
                    else TextGray.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = number.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else TextGray,
                    fontSize = 12.sp
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray.copy(alpha = alpha)
            )
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                actionButton()
            }
        }
    }
}

@Composable
fun DownloadedUpdatesSection(
    downloadedApks: List<File>,
    onInstall: (File, String) -> Unit,
    onDelete: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    if (downloadedApks.isEmpty()) return

    var fileToDelete by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "النسخ المنزلة سابقاً",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        downloadedApks.forEach { file ->
            val versionName = remember(file.name) {
                file.name.removePrefix("Qdash-v").removeSuffix(".apk")
            }
            
            val fileSize = remember(file) {
                com.qdash.core.utils.FormatterUtils.convertNumerals(String.format("%.2f MB", file.length().toDouble() / (1024 * 1024)))
            }
            val downloadDate = remember(file) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                com.qdash.core.utils.FormatterUtils.convertNumerals(sdf.format(java.util.Date(file.lastModified())))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إصدار v$versionName",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = fileSize, style = MaterialTheme.typography.bodySmall, color = TextGray)
                            Box(modifier = Modifier.size(3.dp).background(TextGray.copy(alpha = 0.5f), CircleShape))
                            Text(text = downloadDate, style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onInstall(file, versionName) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = IncomeGreen)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "تثبيت")
                        }
                        IconButton(
                            onClick = { fileToDelete = file },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = ExpenseRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("حذف ملف التحديث", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا الملف لتحرير مساحة التخزين؟ لن تتمكن من تثبيته إلا بعد إعادة تحميله.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { onDelete(it) }
                        fileToDelete = null
                    }
                ) {
                    Text("حذف", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
