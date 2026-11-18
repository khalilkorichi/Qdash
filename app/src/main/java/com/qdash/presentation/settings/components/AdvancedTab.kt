package com.qdash.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qdash.BuildConfig
import com.qdash.core.utils.FormatterUtils
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AdvancedTab(
    customGeminiApiKey: String = "",
    customOpenRouterApiKey: String = "",
    onSaveCustomApiKeys: (geminiKey: String, openRouterKey: String) -> Unit = { _, _ -> },
    onClearCustomApiKeys: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    onResetOnboarding: (() -> Unit)? = null
) {
    val Primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val bottomBarPadding = LocalBottomBarPadding.current

    var showWipeDataDialog by remember { mutableStateOf(false) }
    var showFullResetDialog by remember { mutableStateOf(false) }
    var showApiKeysDialog by remember { mutableStateOf(false) }
    var dbSizeText by remember { mutableStateOf("...") }

    LaunchedEffect(context) {
        dbSizeText = withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath("kdach_database")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            val totalBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
                (if (walFile.exists()) walFile.length() else 0L) +
                (if (shmFile.exists()) shmFile.length() else 0L)
            FormatterUtils.formatFileSize(totalBytes)
        }
    }

    val hasCustomKeys = customGeminiApiKey.isNotBlank() || customOpenRouterApiKey.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSectionTitle("الذكاء الاصطناعي المتقدم")

        SettingsNavItem(
            icon = Icons.Default.Key,
            iconTint = Primary,
            title = "مفاتيح الذكاء الاصطناعي الخاصة (API Keys)",
            subtitle = if (hasCustomKeys) "تم تفعيل مفتاح خاص مخصص" else "استخدم مفاتيح Gemini أو OpenRouter الخاصة بك (اختياري)",
            onClick = { showApiKeysDialog = true }
        )

        SettingsSectionTitle("الخصوصية والبيانات")

        SettingsNavItem(
            icon = Icons.Default.DeleteForever,
            iconTint = ExpenseRed,
            title = "مسح كل البيانات",
            subtitle = "حذف جميع المعاملات والحسابات والفئات نهائياً",
            onClick = { showWipeDataDialog = true }
        )

        SettingsNavItem(
            icon = Icons.Default.RestartAlt,
            iconTint = SavingsAmber,
            title = "إعادة الضبط الكامل",
            subtitle = "إرجاع التطبيق إلى الإعدادات الافتراضية الأولى",
            onClick = { showFullResetDialog = true }
        )

        SettingsSectionTitle("التشخيص والتقارير")

        SettingsItem(
            icon = Icons.Default.Storage,
            iconTint = Color(0xFF8B5CF6),
            title = "حجم قاعدة البيانات",
            subtitle = "حجم ملف قاعدة البيانات المحلي (SQLite + WAL)",
            trailing = {
                Text(
                    text = dbSizeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        SettingsItem(
            icon = Icons.Default.BugReport,
            iconTint = TextGray,
            title = "وضع التصحيح",
            subtitle = "عرض السجلات التقنية وتشخيص الأخطاء",
            trailing = {
                Text(
                    text = if (BuildConfig.DEBUG) "مفعّل (Debug)" else "معطّل (Release)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (BuildConfig.DEBUG) Primary else TextGray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )

        // ── Developer Options (debug builds only) ─────────────────────────────
        if (BuildConfig.DEBUG && onResetOnboarding != null) {
            SettingsSectionTitle("Developer Options 🛠️")

            SettingsNavItem(
                icon = Icons.Default.Replay,
                iconTint = Color(0xFF6C63FF),
                title = "إعادة تشغيل Onboarding",
                subtitle = "إعادة تعيين علامات الإعداد الأول — البيانات لا تُحذف",
                onClick = onResetOnboarding
            )
        }

        SettingsSectionTitle("معلومات التطبيق")

        SettingsItem(
            icon = Icons.Default.Info,
            iconTint = Primary,
            title = "الإصدار الحالي",
            subtitle = "قداشّ — نسخة الإنتاج",
            trailing = {
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Spacer(modifier = Modifier.height(bottomBarPadding + 24.dp))
    }

    if (showWipeDataDialog) {
        AppDialog(
            onDismissRequest = { showWipeDataDialog = false },
            title = "تأكيد مسح كل البيانات",
            text = "سيتم حذف جميع المعاملات والحسابات والميزانيات والديون المحفوظة على هذا الجهاز نهائياً ولا يمكن التراجع عن ذلك. هل تريد المتابعة؟",
            confirmButtonText = "نعم، مسح كل البيانات",
            dismissButtonText = "إلغاء",
            isDestructive = true,
            onConfirm = {
                showWipeDataDialog = false
                onResetAllData()
            }
        )
    }

    if (showFullResetDialog) {
        AppDialog(
            onDismissRequest = { showFullResetDialog = false },
            title = "إعادة الضبط الكامل",
            text = "سيتم حذف كافة البيانات المحلية وإعادة ضبط جميع تفضيلات التطبيق إلى حالتها الافتراضية الأولى. هل أنت متأكد؟",
            confirmButtonText = "إعادة الضبط الكامل",
            dismissButtonText = "إلغاء",
            isDestructive = true,
            onConfirm = {
                showFullResetDialog = false
                onResetAllData()
            }
        )
    }

    if (showApiKeysDialog) {
        CustomApiKeysDialog(
            initialGeminiKey = customGeminiApiKey,
            initialOpenRouterKey = customOpenRouterApiKey,
            onDismiss = { showApiKeysDialog = false },
            onSave = { gemini, openRouter ->
                onSaveCustomApiKeys(gemini, openRouter)
                showApiKeysDialog = false
            },
            onClear = {
                onClearCustomApiKeys()
                showApiKeysDialog = false
            }
        )
    }
}

@Composable
private fun CustomApiKeysDialog(
    initialGeminiKey: String,
    initialOpenRouterKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit
) {
    var geminiInput by remember(initialGeminiKey) { mutableStateOf(initialGeminiKey) }
    var openRouterInput by remember(initialOpenRouterKey) { mutableStateOf(initialOpenRouterKey) }
    var showGemini by remember { mutableStateOf(false) }
    var showOpenRouter by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "مفاتيح الذكاء الاصطناعي الخاصة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "تُحفظ مفاتيحك محلياً على جهازك وتُعطى الأولوية عند الاتصال بنماذج Gemini و OpenRouter. اترك الحقل فارغاً لاستخدام المفتاح الافتراضي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = geminiInput,
                    onValueChange = { geminiInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (showGemini) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGemini = !showGemini }) {
                            Icon(
                                imageVector = if (showGemini) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = openRouterInput,
                    onValueChange = { openRouterInput = it },
                    label = { Text("OpenRouter API Key") },
                    placeholder = { Text("sk-or-v1-...") },
                    singleLine = true,
                    visualTransformation = if (showOpenRouter) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOpenRouter = !showOpenRouter }) {
                            Icon(
                                imageVector = if (showOpenRouter) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (initialGeminiKey.isNotBlank() || initialOpenRouterKey.isNotBlank()) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح المفاتيح المحفوظة")
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onSave(geminiInput, openRouterInput) },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
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
        }
    )
}
