package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController = compositionLocalOf<NavHostController?> { null }

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "الترحيب")
    object Home : Screen("home", "الرئيسية", Icons.Default.Home)
    object AddTransaction : Screen("add_transaction?type={type}&transactionId={transactionId}&date={date}&draft={draft}", "إضافة عملية", Icons.Default.AddCircle) {
        fun createRoute(type: String, transactionId: Long? = null, date: Long? = null, draft: String? = null) =
            "add_transaction?type=$type" + 
            (if (transactionId != null) "&transactionId=$transactionId" else "") +
            (if (date != null) "&date=$date" else "") +
            (if (draft != null) run {
                val encoded = try {
                    java.net.URLEncoder.encode(draft, "UTF-8")
                } catch (e: Exception) {
                    draft
                }
                "&draft=$encoded"
            } else "")
    }
    object Transactions : Screen("transactions", "المعاملات", Icons.Default.History)
    object Accounts : Screen("accounts", "الحسابات", Icons.Default.AccountBalanceWallet)
    object Income : Screen("income", "الدخل", Icons.Default.TrendingUp)
    object Savings : Screen("savings", "الادخار", Icons.Default.Savings)
    object Subscriptions : Screen("subscriptions", "الاشتراكات", Icons.Default.Receipt)
    object Analytics : Screen("analytics", "الإحصائيات", Icons.Default.SignalCellularAlt)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
    
    // Budget Goals Feature Screens
    object BudgetGoals : Screen("budget_goals", "أهداف الميزانية", Icons.Default.PieChart)
    object AddBudgetGoal : Screen("add_budget_goal", "إضافة ميزانية")
    object BudgetGoalDetails : Screen("budget_goal_details/{budgetId}", "تفاصيل الميزانية") {
        fun createRoute(budgetId: Long) = "budget_goal_details/$budgetId"
    }
    
    // New Features Screens
    object Debts : Screen("debts", "الديون والالتزامات", Icons.Default.CreditCard)
    object Transfer : Screen("transfer", "تحويل الأرصدة", Icons.Default.SyncAlt)
    object Export : Screen("export", "تصدير التقارير", Icons.Default.PictureAsPdf)
    object Salary : Screen("salary", "إدارة الراتب", Icons.Default.AccountBalanceWallet)
    
    // Discovery & Utility Screens
    object Notifications : Screen("notifications", "الإشعارات", Icons.Default.Notifications)
    object Search : Screen("search", "البحث", Icons.Default.Search)
    object Categories : Screen("categories", "الفئات", Icons.Default.Category)
    object FinancialPlans : Screen("financial_plans", "الخطط المالية", Icons.Default.EventNote)
    
    // New screens (Phase 2)
    object Backup : Screen("backup", "النسخ الاحتياطي والاستعادة", Icons.Default.Backup)
    object IncomeHistory : Screen("income_history", "سجل المداخيل", Icons.Default.History)
    
    // Transaction Templates
    object Templates : Screen("templates", "قوالب المعاملات", Icons.Default.ReceiptLong)
    object CreateTemplate : Screen("create_template", "قالب جديد")
    object EditTemplate : Screen("edit_template/{templateId}", "تعديل القالب") {
        fun createRoute(templateId: Long) = "edit_template/$templateId"
    }

    // AI Chat Screen
    object AiChat : Screen("ai_chat", "المساعد الذكي قداشّ", Icons.Default.Android)
}

val mainBottomNavScreens = listOf(
    Screen.Home,
    Screen.Analytics,
    Screen.Accounts,
    Screen.Settings
)
