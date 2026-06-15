package com.example.core.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * PreferencesManager wraps all SharedPreferences files used by the application,
 * providing type-safe properties and encapsulating key names and files.
 * It also handles consolidation of duplicated keys (e.g. show_balance_total) and cross-file issues.
 */
class PreferencesManager(context: Context) {
    private val safeContext = context.applicationContext ?: context
    private val mainPrefs: SharedPreferences = safeContext.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
    private val dashboardPrefs: SharedPreferences = safeContext.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
    private val searchPrefs: SharedPreferences = safeContext.getSharedPreferences("fintrack_search_prefs", Context.MODE_PRIVATE)

    private val _dashboardConfigUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dashboardConfigUpdates: SharedFlow<Unit> = _dashboardConfigUpdates.asSharedFlow()

    fun notifyDashboardConfigChanged() {
        _dashboardConfigUpdates.tryEmit(Unit)
    }

    // A) kdach_prefs Keys

    var isFirstLaunch: Boolean
        get() = mainPrefs.getBoolean("is_first_launch", true)
        set(value) = mainPrefs.edit().putBoolean("is_first_launch", value).apply()

    var hideDecimalsEnabled: Boolean
        get() = mainPrefs.getBoolean("hide_decimals_enabled", false)
        set(value) = mainPrefs.edit().putBoolean("hide_decimals_enabled", value).apply()

    var lastBackupDate: String
        get() = mainPrefs.getString("last_backup_date", "غير متوفر") ?: "غير متوفر"
        set(value) = mainPrefs.edit().putString("last_backup_date", value).apply()

    var autoBackupEnabled: Boolean
        get() = mainPrefs.getBoolean("auto_backup_enabled", false)
        set(value) = mainPrefs.edit().putBoolean("auto_backup_enabled", value).apply()

    var connectedEmail: String?
        get() = mainPrefs.getString("connected_email", null)
        set(value) = mainPrefs.edit().putString("connected_email", value).apply()

    var darkModeEnabled: Boolean
        get() = mainPrefs.getBoolean("dark_mode_enabled", false)
        set(value) = mainPrefs.edit().putBoolean("dark_mode_enabled", value).apply()

    var amountWordsEnabled: Boolean
        get() = mainPrefs.getBoolean("amount_words_enabled", true)
        set(value) = mainPrefs.edit().putBoolean("amount_words_enabled", value).apply()

    var selectedAiModel: String
        get() = mainPrefs.getString("selected_ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) = mainPrefs.edit().putString("selected_ai_model", value).apply()

    var notificationPermissionHandled: Boolean
        get() = mainPrefs.getBoolean("notification_permission_handled", false)
        set(value) = mainPrefs.edit().putBoolean("notification_permission_handled", value).apply()

    var notificationPermissionGranted: Boolean
        get() = mainPrefs.getBoolean("notification_permission_granted", false)
        set(value) = mainPrefs.edit().putBoolean("notification_permission_granted", value).apply()

    var walletSetupCompleted: Boolean
        get() = mainPrefs.getBoolean("wallet_setup_completed", false)
        set(value) = mainPrefs.edit().putBoolean("wallet_setup_completed", value).apply()

    var appLanguage: String
        get() = mainPrefs.getString("app_language", "ar") ?: "ar"
        set(value) = mainPrefs.edit().putString("app_language", value).apply()

    var useWesternNumerals: Boolean
        get() = mainPrefs.getBoolean("use_western_numerals", true)
        set(value) = mainPrefs.edit().putBoolean("use_western_numerals", value).apply()

    var lastNotifiedUpdateVersion: String
        get() = mainPrefs.getString("last_notified_update_version", "") ?: ""
        set(value) = mainPrefs.edit().putString("last_notified_update_version", value).apply()

    fun getShowBalanceAcc(accountId: Long): Boolean {
        return mainPrefs.getBoolean("show_balance_acc_$accountId", true)
    }

    fun setShowBalanceAcc(accountId: Long, visible: Boolean) {
        mainPrefs.edit().putBoolean("show_balance_acc_$accountId", visible).apply()
        notifyDashboardConfigChanged()
    }


    // B) fintrack_prefs Keys

    var walletSetupReminderDismissed: Boolean
        get() = dashboardPrefs.getBoolean("wallet_setup_reminder_dismissed", false)
        set(value) {
            dashboardPrefs.edit().putBoolean("wallet_setup_reminder_dismissed", value).apply()
            notifyDashboardConfigChanged()
        }

    var dashboardSectionsOrder: String
        get() = dashboardPrefs.getString("dashboard_sections_order", "split_cards,context_templates,templates,quick_actions,accounts,chart,budget,subscriptions,recent_transactions") ?: "split_cards,context_templates,templates,quick_actions,accounts,chart,budget,subscriptions,recent_transactions"
        set(value) {
            dashboardPrefs.edit().putString("dashboard_sections_order", value).apply()
            notifyDashboardConfigChanged()
        }

    fun isSectionVisible(section: String): Boolean {
        return dashboardPrefs.getBoolean("dashboard_show_$section", true)
    }

    fun setSectionVisible(section: String, visible: Boolean) {
        dashboardPrefs.edit().putBoolean("dashboard_show_$section", visible).apply()
        notifyDashboardConfigChanged()
    }

    var smartCategorySortEnabled: Boolean
        get() = dashboardPrefs.getBoolean("smart_category_sort_enabled", false)
        set(value) = dashboardPrefs.edit().putBoolean("smart_category_sort_enabled", value).apply()


    // C) fintrack_search_prefs Keys

    var recentSearches: String
        get() = searchPrefs.getString("recent_searches", "") ?: ""
        set(value) = searchPrefs.edit().putString("recent_searches", value).apply()


    // D) Consolidating show_balance_total to mainPrefs (kdach_prefs)
    var showBalanceTotal: Boolean
        get() {
            return if (mainPrefs.contains("show_balance_total")) {
                mainPrefs.getBoolean("show_balance_total", true)
            } else if (dashboardPrefs.contains("show_balance_total")) {
                val legacyVal = dashboardPrefs.getBoolean("show_balance_total", true)
                mainPrefs.edit().putBoolean("show_balance_total", legacyVal).apply()
                dashboardPrefs.edit().remove("show_balance_total").apply()
                legacyVal
            } else {
                true
            }
        }
        set(value) {
            mainPrefs.edit().putBoolean("show_balance_total", value).apply()
            if (dashboardPrefs.contains("show_balance_total")) {
                dashboardPrefs.edit().remove("show_balance_total").apply()
            }
            notifyDashboardConfigChanged()
        }

    // E) Consolidating wallet_setup_skipped to mainPrefs (kdach_prefs)
    var walletSetupSkipped: Boolean
        get() {
            return if (mainPrefs.contains("wallet_setup_skipped")) {
                mainPrefs.getBoolean("wallet_setup_skipped", false)
            } else if (dashboardPrefs.contains("wallet_setup_skipped")) {
                val legacyVal = dashboardPrefs.getBoolean("wallet_setup_skipped", false)
                mainPrefs.edit().putBoolean("wallet_setup_skipped", legacyVal).apply()
                dashboardPrefs.edit().remove("wallet_setup_skipped").apply()
                legacyVal
            } else {
                false
            }
        }
        set(value) {
            mainPrefs.edit().putBoolean("wallet_setup_skipped", value).apply()
            if (dashboardPrefs.contains("wallet_setup_skipped")) {
                dashboardPrefs.edit().remove("wallet_setup_skipped").apply()
            }
            notifyDashboardConfigChanged()
        }
}
