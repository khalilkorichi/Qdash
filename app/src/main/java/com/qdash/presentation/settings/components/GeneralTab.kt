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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.UserProfile
import com.qdash.presentation.settings.AboutDialog
import com.qdash.presentation.settings.SettingsUiState
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TransferBlue

@Composable
fun GeneralTab(
    uiState: SettingsUiState,
    userProfile: UserProfile?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onToggleDark: (Boolean) -> Unit,
    onToggleHideDecimals: (Boolean) -> Unit,
    onToggleAmountWords: (Boolean) -> Unit,
    onToggleWesternNumerals: (Boolean) -> Unit,
    onNavigateToBudgetGoals: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToFinancialPlans: () -> Unit,
    onNavigateToSalary: () -> Unit,
    onNavigateToAccountManagement: () -> Unit,
    onCustomiseDashboardClick: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var showAboutDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // My Account Profile Card
        UserProfileCard(
            userProfile = userProfile,
            lastSyncTimestamp = uiState.lastSyncTimestamp,
            onClick = onNavigateToAccountManagement
        )

        // Google Sign-In / Sign-Out Button
        GoogleSignInOutButton(
            isLinked = userProfile?.isGoogleLinked == true,
            isLoading = uiState.isSyncing,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            SettingsSectionTitle("المظهر واللغة")
            SettingsItemSkeleton()
            SettingsItemSkeleton()
            SettingsItemSkeleton()
            SettingsSectionTitle("الأدوات والإدارة")
            repeat(6) { SettingsItemSkeleton() }
            Spacer(modifier = Modifier.height(96.dp))
        } else {
            SettingsSectionTitle("المظهر واللغة")

            SettingsItem(
                icon = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconTint = Color(0xFF7C3AED),
                title = "الوضع المظلم",
                subtitle = if (uiState.isDarkTheme) "وضع الليل مفعّل" else "وضع النهار مفعّل",
                trailing = {
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = onToggleDark,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.MonetizationOn,
                iconTint = IncomeGreen,
                title = "العملة الأساسية",
                subtitle = "الدينار الجزائري (DZD)",
                trailing = {
                    Text("DZD", style = MaterialTheme.typography.labelLarge, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            )

            SettingsItem(
                icon = Icons.Default.MoneyOff,
                iconTint = SavingsAmber,
                title = "إخفاء الفواصل الصفرية",
                subtitle = "إزالة الصفرين (00.) من عرض المبالغ المالية",
                trailing = {
                    Switch(
                        checked = uiState.isHideDecimalsEnabled,
                        onCheckedChange = onToggleHideDecimals,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.TextFields,
                iconTint = TransferBlue,
                title = "توضيح المبلغ بالحروف",
                subtitle = "عرض المبلغ بالدينار والسنتيم كتابةً",
                trailing = {
                    Switch(
                        checked = uiState.isAmountWordsEnabled,
                        onCheckedChange = onToggleAmountWords,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.Language,
                iconTint = Primary,
                title = "نظام الأرقام",
                subtitle = if (uiState.useWesternNumerals) "الأرقام الغربية (0-9)" else "الأرقام العربية (٠-٩)",
                trailing = {
                    Switch(
                        checked = uiState.useWesternNumerals,
                        onCheckedChange = onToggleWesternNumerals,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsSectionTitle("الأدوات والإدارة")

            val navController = com.qdash.presentation.navigation.LocalNavController.current
            SettingsNavItem(
                icon = Icons.Default.ReceiptLong,
                iconTint = Primary,
                title = "قوالب المعاملات",
                subtitle = "حفظ المعاملات المتكررة لسرعة تسجيلها",
                onClick = { navController?.navigate(com.qdash.presentation.navigation.Screen.Templates.route) }
            )

            SettingsNavItem(
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = IncomeGreen,
                title = "إدارة الراتب",
                subtitle = "إدارة مصادر الدخل وتوزيعها تلقائياً",
                onClick = onNavigateToSalary
            )

            SettingsNavItem(
                icon = Icons.Default.PieChart,
                iconTint = Primary,
                title = "أهداف الميزانية",
                subtitle = "تتبع الإنفاق الذكي والتنبيهات",
                onClick = onNavigateToBudgetGoals
            )

            SettingsNavItem(
                icon = Icons.Default.CreditCard,
                iconTint = ExpenseRed,
                title = "خطة الديون",
                subtitle = "جدولة وتسوية الالتزامات المالية",
                onClick = onNavigateToDebts
            )

            SettingsNavItem(
                icon = Icons.Default.SyncAlt,
                iconTint = TransferBlue,
                title = "تحويل الرصيد",
                subtitle = "تحويل الأرصدة بين الحسابات",
                onClick = onNavigateToTransfer
            )

            SettingsNavItem(
                icon = Icons.Default.PictureAsPdf,
                iconTint = Color(0xFFDC2626),
                title = "تصدير التقارير PDF",
                subtitle = "كشوف الحساب والتحليلات",
                onClick = onNavigateToExport
            )

            SettingsNavItem(
                icon = Icons.Default.Flag,
                iconTint = IncomeGreen,
                title = "الخطط المالية",
                subtitle = "خطط الادخار والأهداف طويلة الأمد",
                onClick = onNavigateToFinancialPlans
            )

            SettingsSectionTitle("تخصيص الواجهة")

            SettingsNavItem(
                icon = Icons.Default.Tune,
                iconTint = Primary,
                title = "ترتيب وإخفاء أقسام الواجهة",
                subtitle = "تخصيص البطاقات والترتيب في الصفحة الرئيسية",
                onClick = onCustomiseDashboardClick
            )

            SettingsSectionTitle("عن التطبيق")

            SettingsLogoNavItem(
                painter = painterResource(id = com.qdash.R.drawable.ic_app_logo),
                title = "نسخة التطبيق",
                subtitle = "قداشّ — الإصدار التجاري",
                onClick = { showAboutDialog = true }
            )

            if (showAboutDialog) {
                AboutDialog(onDismiss = { showAboutDialog = false })
            }

            SettingsNavItem(
                icon = Icons.Default.SystemUpdate,
                iconTint = Primary,
                title = "تحديثات التطبيق",
                subtitle = "التحقق من وجود تحديثات جديدة وتثبيتها",
                onClick = { navController?.navigate(com.qdash.presentation.navigation.Screen.Updates.route) }
            )

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
