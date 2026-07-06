package com.qdash.presentation.backup.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.BackupProgress
import com.qdash.domain.model.RestorePreview
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens

@Composable
fun ProcessingOverlayDialog(isLoading: Boolean) {
    if (isLoading) {
        val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
        val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight

        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = ShapeTokens.Lg,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .width(280.dp)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, ShapeTokens.Lg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(rotation)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "جاري معالجة البيانات...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "يرجى الانتظار ولا تغلق الصفحة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupProgressDialog(
    backupProgress: BackupProgress,
    onClearProgress: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    when (backupProgress) {
        is BackupProgress.Running -> {
            AlertDialog(
                onDismissRequest = { /* Non-dismissable */ },
                title = {
                    Text(
                        text = "جاري إنشاء النسخة الاحتياطية...",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = backupProgress.stage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        LinearProgressIndicator(
                            progress = { backupProgress.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = primaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${backupProgress.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }
        is BackupProgress.Failure -> {
            AlertDialog(
                onDismissRequest = onClearProgress,
                title = {
                    Text(
                        text = "فشل النسخ الاحتياطي",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(
                        text = backupProgress.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = onClearProgress) {
                        Text("موافق", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonBottomSheet(
    showComingSoonSheet: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showComingSoonSheet) {
        val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
        val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), ShapeTokens.Md),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "المزامنة السحابية (Google Drive) - قريباً",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "الميزة قيد التطوير حالياً. لضمان أقصى درجات الخصوصية والأمان المالي وحماية بياناتك الحساسة، تم تفعيل النسخ الاحتياطي اليدوي الكامل والمشفّر محلياً (BackupFormatV2).\n\nسنقوم بتوفير إمكانية الربط السحابي المباشر بمجرد اكتمال اختبارات المزامنة والتحقق الأمني.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondaryColor,
                    textAlign = TextAlign.Center
                )

                AppButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg
                ) {
                    Text("فهمت ذلك", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FolderBackupOptionsDialog(
    showFolderBackupPasswordDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (password: CharArray?) -> Unit
) {
    if (showFolderBackupPasswordDialog) {
        var exportEncrypt by remember { mutableStateOf(false) }
        var exportPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("خيارات النسخ الاحتياطي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = exportEncrypt,
                            onCheckedChange = { exportEncrypt = it }
                        )
                        Text("تشفير النسخة الاحتياطية بكلمة مرور")
                    }

                    if (exportEncrypt) {
                        OutlinedTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = { Text("كلمة المرور") },
                            placeholder = { Text("أدخل كلمة مرور قوية") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "تنبيه: في حال نسيان كلمة المرور، لن تتمكن من استرجاع بياناتك أبداً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.Danger
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (exportEncrypt && exportPassword.isBlank()) return@TextButton
                        onConfirm(if (exportEncrypt) exportPassword.toCharArray() else null)
                    }
                ) {
                    Text("بدء النسخ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ExportOptionsDialog(
    showExportDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (password: CharArray?) -> Unit
) {
    if (showExportDialog) {
        var exportEncrypt by remember { mutableStateOf(false) }
        var exportPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("خيارات التصدير الفردي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = exportEncrypt,
                            onCheckedChange = { exportEncrypt = it }
                        )
                        Text("تشفير النسخة الاحتياطية بكلمة مرور")
                    }

                    if (exportEncrypt) {
                        OutlinedTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = { Text("كلمة المرور") },
                            placeholder = { Text("أدخل كلمة مرور قوية") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "تنبيه: في حال نسيان كلمة المرور، لن تتمكن من استرجاع بياناتك أبداً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.Danger
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (exportEncrypt && exportPassword.isBlank()) return@TextButton
                        onConfirm(if (exportEncrypt) exportPassword.toCharArray() else null)
                    }
                ) {
                    Text("تصدير", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ImportPasswordDialog(
    showPasswordPrompt: Boolean,
    passwordError: String?,
    onDismissRequest: () -> Unit,
    onConfirm: (password: CharArray) -> Unit
) {
    if (showPasswordPrompt) {
        var restorePasswordInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("الملف مشفر", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى إدخال كلمة المرور لفك تشفير بيانات النسخة الاحتياطية.")
                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) {
                        Text(
                            passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (restorePasswordInput.isNotBlank()) {
                            onConfirm(restorePasswordInput.toCharArray())
                        }
                    }
                ) {
                    Text("تحقق وفك التشفير", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun RestorePreviewDialog(
    restorePreview: RestorePreview?,
    onDismissRequest: () -> Unit,
    onConfirm: (selectedTables: List<String>) -> Unit
) {
    if (restorePreview != null) {
        val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
        val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight

        var restoreAccountsAndTransactions by remember { mutableStateOf(true) }
        var restoreSavingsAndGoals by remember { mutableStateOf(true) }
        var restoreDebts by remember { mutableStateOf(true) }
        var restoreAdvanced by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("معاينة بيانات النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("تاريخ النسخة: ${FormatterUtils.formatDate(restorePreview.manifest.createdAt)}")
                    Text("إصدار التطبيق: ${restorePreview.manifest.appVersion}")
                    Text("إصدار الصيغة: ${restorePreview.manifest.schemaVersion}")

                    HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else ColorTokens.BorderLight)

                    Text("اختر البيانات المراد استعادتها:", fontWeight = FontWeight.Bold)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = restoreAccountsAndTransactions,
                            onCheckedChange = { restoreAccountsAndTransactions = it }
                        )
                        Column {
                            Text("الحسابات والعمليات والتحويلات", fontWeight = FontWeight.Bold)
                            val txCount = restorePreview.manifest.recordCounts["transactions"] ?: 0
                            val accCount = restorePreview.manifest.recordCounts["accounts"] ?: 0
                            Text("يحتوي على: $accCount حسابات، $txCount عمليات", fontSize = 11.sp, color = textSecondaryColor)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = restoreSavingsAndGoals,
                            onCheckedChange = { restoreSavingsAndGoals = it }
                        )
                        Column {
                            Text("أهداف الادخار والاشتراكات", fontWeight = FontWeight.Bold)
                            val sCount = restorePreview.manifest.recordCounts["saving_goals"] ?: 0
                            val subCount = restorePreview.manifest.recordCounts["subscriptions"] ?: 0
                            Text("يحتوي على: $sCount أهداف ادخار، $subCount اشتراكات", fontSize = 11.sp, color = textSecondaryColor)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = restoreDebts,
                            onCheckedChange = { restoreDebts = it }
                        )
                        Column {
                            Text("الديون والمدفوعات المتعلقة بها", fontWeight = FontWeight.Bold)
                            val debtCount = restorePreview.manifest.recordCounts["debts"] ?: 0
                            Text("يحتوي على: $debtCount ديون مسجلة", fontSize = 11.sp, color = textSecondaryColor)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = restoreAdvanced,
                            onCheckedChange = { restoreAdvanced = it }
                        )
                        Column {
                            Text("القوالب، الإعدادات، والميزات الأخرى", fontWeight = FontWeight.Bold)
                            val templateCount = restorePreview.manifest.recordCounts["transaction_templates"] ?: 0
                            val profileCount = restorePreview.manifest.recordCounts["postal_profiles"] ?: 0
                            Text("يحتوي على: $templateCount قوالب، $profileCount ملفات بريدية، إلخ", fontSize = 11.sp, color = textSecondaryColor)
                        }
                    }

                    if (!restorePreview.isCompatible) {
                        Text(
                            "تحذير: إصدار صيغة النسخة الاحتياطية أحدث من إصدار التطبيق الحالي وقد تظهر مشاكل أثناء الاستعادة.",
                            color = ColorTokens.Danger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTables = mutableListOf<String>()
                        if (restoreAccountsAndTransactions) {
                            selectedTables.addAll(listOf("accounts", "categories", "transactions", "transfers", "category_rules", "user_category_mappings"))
                        }
                        if (restoreSavingsAndGoals) {
                            selectedTables.addAll(listOf("saving_goals", "savings_contributions", "subscriptions"))
                        }
                        if (restoreDebts) {
                            selectedTables.addAll(listOf("debts", "debt_payments"))
                        }
                        if (restoreAdvanced) {
                            selectedTables.addAll(listOf("income_sources", "budget_goals", "financial_plans", "transaction_templates", "notifications", "ai_chat_messages", "postal_profiles", "salary_delays", "salary_distributions", "salary_envelopes"))
                        }

                        if (selectedTables.isNotEmpty()) {
                            onConfirm(selectedTables)
                        }
                    }
                ) {
                    Text("استعادة البيانات المحددة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("إلغاء")
                }
            }
        )
    }
}
