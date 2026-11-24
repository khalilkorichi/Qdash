package com.qdash.presentation.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.qdash.core.utils.FormatterUtils
import com.qdash.presentation.backup.components.RestoreBackupPromptDialog
import com.qdash.presentation.settings.components.GoogleSignInOutButton
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    viewModel: AccountManagementViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pendingCloudBackup by viewModel.pendingCloudBackup.collectAsStateWithLifecycle()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showBirthdatePicker by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.connectGoogleAccount(
                    account = account,
                    onSuccess = {
                        Toast.makeText(context, "تم ربط حساب Google بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "فشل ربط الحساب: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تسجيل الدخول: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val launchGoogleSignIn = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val rawBirthdate = userProfile?.birthDate?.trim().orEmpty()
    val parsedBirthdateMillis: Long? = remember(rawBirthdate) {
        if (rawBirthdate.isBlank()) null else {
            runCatching { isoFormat.parse(rawBirthdate)?.time }.getOrNull()
        }
    }
    val displayBirthdate = remember(rawBirthdate, parsedBirthdateMillis) {
        when {
            parsedBirthdateMillis != null -> FormatterUtils.formatDate(parsedBirthdateMillis)
            rawBirthdate.isNotBlank() -> rawBirthdate
            else -> ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة الحساب",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isLinked = userProfile?.isGoogleLinked == true
            val name = userProfile?.name ?: "ضيف قداشّ"
            val email = userProfile?.email
            val avatarUrl = userProfile?.avatarUrl

            // 1. Account Details Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "الصورة الشخصية",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!email.isNullOrEmpty()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }

                    // Google badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLinked) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFF6B7280).copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = if (isLinked) Color(0xFF22C55E).copy(alpha = 0.3f) else Color(0xFF6B7280).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isLinked) Color(0xFF22C55E) else Color(0xFF6B7280), CircleShape)
                            )
                            Text(
                                text = if (isLinked) "حساب متصل سحابياً" else "وضع حساب ضيف",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLinked) Color(0xFF22C55E) else TextGray
                            )
                        }
                    }
                }
            }

            // Guest Mode: Link Google Account button right here
            if (!isLinked) {
                GoogleSignInOutButton(
                    isLinked = false,
                    isLoading = isSyncing,
                    onSignInClick = launchGoogleSignIn,
                    onSignOutClick = {}
                )
            }

            // 2. Birthdate Picker Card (Clickable row opening AppDatePickerDialog)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تاريخ الميلاد (اختياري)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Text(
                        text = "اختر تاريخ ميلادك لحفظه محلياً في جهازك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeTokens.Md)
                            .clickable { showBirthdatePicker = true },
                        shape = ShapeTokens.Md,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = displayBirthdate.ifEmpty { "لم يُحدَّد بعد — اضغط للاختيار" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (displayBirthdate.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (displayBirthdate.isNotEmpty()) MaterialTheme.colorScheme.onSurface else TextGray
                                )
                            }
                            if (displayBirthdate.isNotEmpty()) {
                                AppCircleIconButton(
                                    icon = Icons.Default.Clear,
                                    contentDescription = "مسح التاريخ",
                                    onClick = { viewModel.saveBirthDate("") },
                                    size = 30.dp,
                                    iconSize = 16.dp,
                                    tint = TextGray
                                )
                            }
                        }
                    }
                }
            }

            // 3. Unlink Action
            if (isLinked) {
                Spacer(modifier = Modifier.height(24.dp))
                AppButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.DANGER
                ) {
                    Text("قطع ربط حساب Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showBirthdatePicker) {
        AppDatePickerDialog(
            initialSelectedDateMillis = parsedBirthdateMillis,
            onDismissRequest = { showBirthdatePicker = false },
            onDateSelected = { selectedMillis ->
                val formattedIso = isoFormat.format(Date(selectedMillis))
                viewModel.saveBirthDate(formattedIso)
                Toast.makeText(context, "تم حفظ تاريخ الميلاد بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (pendingCloudBackup != null) {
        RestoreBackupPromptDialog(
            modifiedTime = pendingCloudBackup!!.modifiedTime,
            isLoading = isSyncing,
            onConfirm = {
                viewModel.restoreExistingCloudBackup(
                    onSuccess = {
                        Toast.makeText(context, "تمت استعادة النسخة السحابية بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "فشل الاستعادة: $err", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = {
                viewModel.keepLocalDataAndUpload {
                    Toast.makeText(context, "تم ربط الحساب والاحتفاظ بالبيانات المحلية!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showConfirmDialog) {
        AppDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = "قطع ربط الحساب",
            text = "هل أنت متأكد من رغبتك في قطع ربط هذا الحساب السحابي؟\n\nتنبيه: لن يتم حذف أي بيانات أو نسخ احتياطية محفوظة على Google Drive، سيتم فقط تسجيل الخروج وإلغاء الربط على هذا الجهاز.",
            confirmButtonText = "قطع الربط",
            onConfirm = {
                viewModel.unlinkAccount {
                    showConfirmDialog = false
                    Toast.makeText(context, "تم قطع ربط الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
            dismissButtonText = "إلغاء",
            onDismiss = { showConfirmDialog = false },
            isDestructive = true
        )
    }
}
