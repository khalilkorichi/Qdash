package com.qdash.presentation.settings

import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.qdash.domain.model.UserProfile
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.presentation.settings.components.*
import kotlinx.coroutines.launch

// ─── Tab Definitions ──────────────────────────────────────────────────────────
private data class SettingsTab(val label: String, val icon: ImageVector)

private val settingsTabs = listOf(
    SettingsTab("عام", Icons.Default.Settings),
    SettingsTab("الإشعارات", Icons.Default.Notifications),
    SettingsTab("النسخ الاحتياطي", Icons.Default.Backup),
    SettingsTab("الفئات", Icons.Default.Category),
    SettingsTab("متقدمة", Icons.Default.Tune)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    backupViewModel: com.qdash.presentation.backup.BackupViewModel,
    onNavigateToBudgetGoals: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToFinancialPlans: () -> Unit = {},
    onNavigateToSalary: () -> Unit = {},
    onNavigateToAccountManagement: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userProfile by viewModel.userProfile.collectAsState()
    var showBirthdateDialog by remember { mutableStateOf(false) }
    var birthdateInput by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.connectGoogleDriveAccount(
                    account,
                    onSuccess = { Toast.makeText(context, "تم ربط الحساب بنجاح ومزامنة البيانات!", Toast.LENGTH_SHORT).show() },
                    onFailure = { err -> Toast.makeText(context, "فشل ربط الحساب: $err", Toast.LENGTH_LONG).show() }
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

    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var showDashboardCustomizationDialog by remember { mutableStateOf(false) }

    // Notification toggles
    var notifBillingReminder by remember { mutableStateOf(true) }
    var notifSalaryReminder by remember { mutableStateOf(true) }
    var notifBudgetAlert by remember { mutableStateOf(true) }
    var notifWeeklyReport by remember { mutableStateOf(false) }
    var notifGoalProgress by remember { mutableStateOf(true) }

    val pagerState = rememberPagerState(initialPage = 0) { settingsTabs.size }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            viewModel.refreshSyncTimestamp()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        FinTrackTopBar(title = "الإعدادات")

        // ── Premium Pill Tab Bar (sticky) ────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settingsTabs.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabContent"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabBorder"
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
                                scope.launch { pagerState.animateScrollToPage(index) }
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── Pager ──────────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> GeneralTab(
                    uiState = uiState,
                    userProfile = userProfile,
                    onToggleDark = { viewModel.toggleDarkTheme(it) },
                    onToggleHideDecimals = { viewModel.toggleHideDecimals(it) },
                    onToggleAmountWords = { viewModel.toggleAmountWords(it) },
                    onToggleWesternNumerals = { viewModel.toggleWesternNumerals(it) },
                    onNavigateToBudgetGoals = onNavigateToBudgetGoals,
                    onNavigateToDebts = onNavigateToDebts,
                    onNavigateToTransfer = onNavigateToTransfer,
                    onNavigateToExport = onNavigateToExport,
                    onNavigateToFinancialPlans = onNavigateToFinancialPlans,
                    onNavigateToSalary = onNavigateToSalary,
                    onNavigateToAccountManagement = onNavigateToAccountManagement,
                    onCustomiseDashboardClick = { showDashboardCustomizationDialog = true }
                )
                1 -> NotificationsTab(
                    billingReminder = notifBillingReminder,
                    salaryReminder = notifSalaryReminder,
                    budgetAlert = notifBudgetAlert,
                    weeklyReport = notifWeeklyReport,
                    goalProgress = notifGoalProgress,
                    onBillingToggle = { notifBillingReminder = it },
                    onSalaryToggle = { notifSalaryReminder = it },
                    onBudgetToggle = { notifBudgetAlert = it },
                    onWeeklyToggle = { notifWeeklyReport = it },
                    onGoalToggle = { notifGoalProgress = it }
                )
                2 -> com.qdash.presentation.backup.BackupScreen(
                    viewModel = backupViewModel,
                    onBack = {},
                    showTopBar = false
                )
                3 -> CategoriesTab(onNavigateToCategories = onNavigateToCategories)
                4 -> AdvancedTab()
            }
        }
    }

    // ─── Confirm Restore Dialog ───────────────────────────────────────────────
    if (showConfirmRestoreDialog) {
        val context2 = LocalContext.current
        ConfirmRestoreDialog(
            onDismiss = { showConfirmRestoreDialog = false },
            onConfirm = {
                viewModel.runRestore(
                    onSuccess = { Toast.makeText(context2, "تمت استعادة بياناتك بنجاح!", Toast.LENGTH_LONG).show() },
                    onFailure = { err -> Toast.makeText(context2, "فشل الاستعادة: $err", Toast.LENGTH_LONG).show() }
                )
                showConfirmRestoreDialog = false
            }
        )
    }

    // ─── Dashboard Customization Dialog ──────────────────────────────────────
    if (showDashboardCustomizationDialog) {
        DashboardCustomizationDialog(
            initialSectionsOrder = uiState.dashboardSectionsOrder,
            initialSectionsVisibility = uiState.dashboardSectionsVisibility,
            onDismiss = { showDashboardCustomizationDialog = false },
            onConfirm = { order, visibility ->
                viewModel.saveDashboardCustomization(order, visibility)
                showDashboardCustomizationDialog = false
                Toast.makeText(context, "تم حفظ الترتيب الجديد بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─── Birthdate Dialog ─────────────────────────────────────────────────────
    if (showBirthdateDialog) {
        BirthdateDialog(
            initialBirthdate = birthdateInput,
            onDismiss = { showBirthdateDialog = false },
            onConfirm = { date ->
                birthdateInput = date
                viewModel.saveBirthDate(date)
                showBirthdateDialog = false
                Toast.makeText(context, "تم حفظ تاريخ الميلاد بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
