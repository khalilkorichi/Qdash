package com.example.presentation.update

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.update.UpdateInfo
import java.io.File

@Composable
fun UpdateBottomBar(
    uiState: UpdateUiState,
    onUpdateClick: (UpdateInfo) -> Unit,
    onPauseClick: (UpdateInfo) -> Unit,
    onResumeClick: (UpdateInfo) -> Unit,
    onInstallClick: (UpdateInfo, File) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = remember(uiState) {
        when (uiState) {
            is UpdateUiState.UpdateAvailable -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)) // Blue Alert
            is UpdateUiState.Downloading, is UpdateUiState.Paused -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)) // Deep Blue Download
            is UpdateUiState.ReadyToInstall, is UpdateUiState.BackupInProgress, is UpdateUiState.BackupSuccess -> listOf(Color(0xFF10B981), Color(0xFF047857)) // Green Install
            is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> listOf(Color(0xFFEF4444), Color(0xFFDC2626)) // Red Error
            is UpdateUiState.FallbackRecovery -> listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Amber Warning
            else -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                // If it's a notification, clicking the bar itself can navigate to full details
                if (uiState is UpdateUiState.UpdateAvailable || uiState is UpdateUiState.DownloadFailed || uiState is UpdateUiState.FallbackRecovery) {
                    onNavigateToUpdates()
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Status Icon
            when (uiState) {
                is UpdateUiState.UpdateAvailable -> {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is UpdateUiState.Downloading -> {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is UpdateUiState.Paused -> {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is UpdateUiState.ReadyToInstall -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is UpdateUiState.BackupInProgress -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is UpdateUiState.BackupSuccess -> {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is UpdateUiState.FallbackRecovery -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {}
            }

            // Central content stack
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState) {
                    is UpdateUiState.UpdateAvailable -> {
                        Text(
                            text = "تحديث جديد متوفر! 🎉",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "الإصدار v${uiState.info.versionName} جاهز للتثبيت الفوري بميزات جديدة.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is UpdateUiState.Downloading -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.speed.isNotEmpty()) "تحميل: ${uiState.speed}" else "جاري تحميل التحديث...",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "${com.example.core.utils.FormatterUtils.convertNumerals(uiState.progress.toString())}%" + if (uiState.eta.isNotEmpty() && uiState.eta != "حساب...") " (${uiState.eta})" else "",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = uiState.progress / 100f,
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                    is UpdateUiState.Paused -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تم إيقاف التحميل مؤقتاً",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "${com.example.core.utils.FormatterUtils.convertNumerals(uiState.progress.toString())}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = uiState.progress / 100f,
                            color = Color.White.copy(alpha = 0.5f),
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Text(
                            text = "اكتمل التحميل! ⚡",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "الإصدار v${uiState.info.versionName} جاهز للاستبدال الآمن والتثبيت.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is UpdateUiState.BackupInProgress -> {
                        Text(
                            text = "جاري حفظ نسخة احتياطية...",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "نقوم بنسخ بياناتك المالية تلقائياً لتأمينها قبل التحديث.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is UpdateUiState.BackupSuccess -> {
                        Text(
                            text = "تم النسخ الاحتياطي بنجاح!",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "جاري تشغيل مثبت الحزم لتثبيت الإصدار الجديد...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is UpdateUiState.DownloadFailed -> {
                        Text(
                            text = "فشل تحميل التحديث",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is UpdateUiState.Error -> {
                        Text(
                            text = "خطأ في الاتصال بالخادم",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is UpdateUiState.FallbackRecovery -> {
                        Text(
                            text = "يتطلب تثبيت يدوي ⚠️",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "يرجى حل مشكلة التثبيت عبر صفحة التفاصيل.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    else -> {}
                }
            }

            // Right action buttons / icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (uiState) {
                    is UpdateUiState.UpdateAvailable -> {
                        Button(
                            onClick = { onUpdateClick(uiState.info) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D4ED8)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("تحديث الآن", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        IconButton(
                            onClick = { onPauseClick(uiState.info) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "إيقاف مؤقت",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    is UpdateUiState.Paused -> {
                        IconButton(
                            onClick = { onResumeClick(uiState.info) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "استئناف",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Button(
                            onClick = { onInstallClick(uiState.info, uiState.localApkFile) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF047857)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("تثبيت الآن", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    is UpdateUiState.DownloadFailed -> {
                        Button(
                            onClick = { onUpdateClick(uiState.info) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFDC2626)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("إعادة المحاولة", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    is UpdateUiState.FallbackRecovery -> {
                        Button(
                            onClick = onNavigateToUpdates,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFD97706)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("حل المشكلة", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    else -> {}
                }

                // Small X dismiss button for everything that is dismissible
                if (uiState !is UpdateUiState.BackupInProgress && uiState !is UpdateUiState.BackupSuccess) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "تجاهل مؤقت",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
