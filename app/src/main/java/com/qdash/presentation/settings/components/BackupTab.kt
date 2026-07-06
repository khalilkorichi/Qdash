package com.qdash.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.DriveSyncCard
import com.qdash.presentation.settings.SettingsUiState
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray

@Composable
fun BackupTab(
    uiState: SettingsUiState,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onConnectGoogleClick: () -> Unit,
    onDisconnectGoogle: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isLoading || uiState.isSyncing) {
            SettingsSectionTitle("النسخ الاحتياطي السحابي")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .shimmerEffect(RoundedCornerShape(16.dp))
            )
            SettingsSectionTitle("إعداد حساب Google Drive")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .shimmerEffect(RoundedCornerShape(16.dp))
            )
            SettingsSectionTitle("إجراءات الاستعادة")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(96.dp))
        } else {
            SettingsSectionTitle("النسخ الاحتياطي السحابي")

            DriveSyncCard(
                lastBackupDate = uiState.lastBackupDate,
                onBackupClick = onBackupClick,
                onRestoreClick = onRestoreClick
            )

            SettingsSectionTitle("إعداد حساب Google Drive")

            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val email = uiState.connectedAccountEmail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (email != null) IncomeGreen else TextGray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (email != null) "حساب Google متصل" else "حساب السحابة غير متصل",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                email ?: "اربط حسابك لحماية بياناتك المالية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                    if (email != null) {
                        AppButton(
                            onClick = onDisconnectGoogle,
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.DANGER
                        ) { Text("إلغاء الربط", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    } else {
                        AppButton(
                            onClick = onConnectGoogleClick,
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.PRIMARY,
                            shape = ShapeTokens.Md
                        ) { Text("ربط الحساب", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            SettingsSectionTitle("إعدادات النسخ التلقائي")

            SettingsItem(
                icon = Icons.Default.CloudSync,
                iconTint = Primary,
                title = "النسخ الاحتياطي التلقائي",
                subtitle = "نسخ تلقائي دوري كل أول الشهر",
                trailing = {
                    Switch(
                        checked = uiState.isAutoBackupEnabled,
                        onCheckedChange = onAutoBackupToggle,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsSectionTitle("إجراءات الاستعادة")

            // Backup now button
            AppButton(
                onClick = onBackupClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg,
                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("نسخ احتياطي الآن", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Restore from backup
            AppButton(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.BORDERED,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg,
                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("استعادة من النسخة الاحتياطية", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
