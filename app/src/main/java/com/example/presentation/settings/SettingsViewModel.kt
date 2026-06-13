package com.example.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import com.example.core.utils.FormatterUtils
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SettingsUiState(
    val lastBackupDate: String = "غير متوفر",
    val isAutoBackupEnabled: Boolean = false,
    val connectedAccountEmail: String? = null,
    val backupRestoreStatus: String? = null,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = false,
    val isDarkTheme: Boolean = false,
    val isHideDecimalsEnabled: Boolean = true,
    val isAmountWordsEnabled: Boolean = true
)

class SettingsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val incomeRepository: IncomeRepository,
    private val savingRepository: SavingRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val database: AppDatabase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBackupPreferences()
    }

    private fun loadBackupPreferences() {
        // Read stats or local caches
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        val date = sharedPrefs.getString("last_backup_date", "غير متوفر") ?: "غير متوفر"
        val autoBackup = sharedPrefs.getBoolean("auto_backup_enabled", false)
        val email = sharedPrefs.getString("connected_email", null)
        val darkTheme = sharedPrefs.getBoolean("dark_mode_enabled", false)
        val hideDecimals = sharedPrefs.getBoolean("hide_decimals_enabled", true)
        val amountWords = sharedPrefs.getBoolean("amount_words_enabled", true)

        FormatterUtils.hideDecimals = hideDecimals

        _uiState.update {
            it.copy(
                lastBackupDate = date,
                isAutoBackupEnabled = autoBackup,
                connectedAccountEmail = email,
                isDarkTheme = darkTheme,
                isHideDecimalsEnabled = hideDecimals,
                isAmountWordsEnabled = amountWords
            )
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        _uiState.update { it.copy(isDarkTheme = enabled) }
    }

    fun toggleHideDecimals(enabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("hide_decimals_enabled", enabled).apply()
        FormatterUtils.hideDecimals = enabled
        _uiState.update { it.copy(isHideDecimalsEnabled = enabled) }
    }

    fun toggleAmountWords(enabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("amount_words_enabled", enabled).apply()
        _uiState.update { it.copy(isAmountWordsEnabled = enabled) }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
    }

    fun connectGoogleDriveAccount(email: String) {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("connected_email", email).apply()
        _uiState.update { it.copy(connectedAccountEmail = email) }
    }

    fun disconnectGoogleDrive() {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("connected_email").apply()
        _uiState.update { it.copy(connectedAccountEmail = null) }
    }

    // JSON Backup System: serializes all tables into a structured JSON file!
    // JSON Backup System: serializes all tables into a structured JSON file!
    fun runBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري تحضير النسخة الاحتياطية...") }
        
        viewModelScope.launch {
            try {
                val backupObj = JSONObject()

                // Capture Accounts
                val accountsArray = JSONArray()
                database.accountDao().getAllAccountsIncludingArchived().first().forEach {
                    accountsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("type", it.type)
                        put("balance", it.balance)
                        put("currency", it.currency)
                        put("color", it.color)
                        put("icon", it.icon)
                        put("isDefault", it.isDefault)
                        put("isArchived", it.isArchived)
                        put("createdAt", it.createdAt)
                    })
                }
                backupObj.put("accounts", accountsArray)

                // Capture Categories
                val catsArray = JSONArray()
                database.categoryDao().getAllCategories().first().forEach {
                    if (!it.isSystem) { // Only backup non-system custom categories
                        catsArray.put(JSONObject().apply {
                            put("id", it.id)
                            put("name", it.name)
                            put("type", it.type)
                            put("icon", it.icon)
                            put("color", it.color)
                            put("budgetLimit", it.budgetLimit ?: JSONObject.NULL)
                            put("isSystem", it.isSystem)
                            put("parentId", it.parentId ?: JSONObject.NULL)
                            put("sortOrder", it.sortOrder)
                        })
                    }
                }
                backupObj.put("categories", catsArray)

                // Capture Transactions
                val txArray = JSONArray()
                database.transactionDao().getAllTransactions().first().forEach {
                    txArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("amount", it.amount)
                        put("type", it.type)
                        put("categoryId", it.categoryId)
                        put("accountId", it.accountId)
                        put("toAccountId", it.toAccountId ?: JSONObject.NULL)
                        put("note", it.note ?: JSONObject.NULL)
                        put("date", it.date)
                        put("isRecurring", it.isRecurring)
                        put("recurringPeriod", it.recurringPeriod ?: JSONObject.NULL)
                        put("attachmentPath", it.attachmentPath ?: JSONObject.NULL)
                    })
                }
                backupObj.put("transactions", txArray)

                // Capture Income Sources
                val incomeArray = JSONArray()
                database.incomeSourceDao().getAllIncomeSources().first().forEach {
                    incomeArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("amount", it.amount)
                        put("type", it.type)
                        put("accountId", it.accountId)
                        put("dayOfMonth", it.dayOfMonth)
                        put("isActive", it.isActive)
                        put("nextExpectedDate", it.nextExpectedDate)
                    })
                }
                backupObj.put("income_sources", incomeArray)

                // Capture Saving Goals
                val savingsGoalsArray = JSONArray()
                database.savingGoalDao().getAllSavingGoals().first().forEach {
                    savingsGoalsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("targetAmount", it.targetAmount)
                        put("currentAmount", it.currentAmount)
                        put("deadline", it.deadline ?: JSONObject.NULL)
                        put("accountId", it.accountId)
                        put("icon", it.icon)
                        put("color", it.color)
                        put("isCompleted", it.isCompleted)
                    })
                }
                backupObj.put("saving_goals", savingsGoalsArray)

                // Capture Savings Contributions
                val savingsContributionsArray = JSONArray()
                database.savingsContributionDao().getAllContributions().first().forEach {
                    savingsContributionsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("savingGoalId", it.savingGoalId)
                        put("accountId", it.accountId)
                        put("amount", it.amount)
                        put("type", it.type)
                        put("note", it.note ?: JSONObject.NULL)
                        put("date", it.date)
                        put("linkedTransactionId", it.linkedTransactionId ?: JSONObject.NULL)
                        put("createdAt", it.createdAt)
                    })
                }
                backupObj.put("savings_contributions", savingsContributionsArray)

                // Capture Subscriptions
                val subscriptionsArray = JSONArray()
                database.subscriptionDao().getAllSubscriptions().first().forEach {
                    subscriptionsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("amount", it.amount)
                        put("currency", it.currency)
                        put("billingCycle", it.billingCycle)
                        put("nextBillingDate", it.nextBillingDate)
                        put("accountId", it.accountId)
                        put("categoryId", it.categoryId)
                        put("icon", it.icon ?: JSONObject.NULL)
                        put("isActive", it.isActive)
                        put("reminderDaysBefore", it.reminderDaysBefore)
                    })
                }
                backupObj.put("subscriptions", subscriptionsArray)

                // Capture Debts
                val debtsArray = JSONArray()
                database.debtDao().getAllDebts().first().forEach {
                    debtsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("title", it.title)
                        put("creditorName", it.creditorName)
                        put("totalAmount", it.totalAmount)
                        put("remainingAmount", it.remainingAmount)
                        put("interestRate", it.interestRate ?: JSONObject.NULL)
                        put("dueDate", it.dueDate ?: JSONObject.NULL)
                        put("minimumPayment", it.minimumPayment)
                        put("recommendedPayment", it.recommendedPayment ?: JSONObject.NULL)
                        put("paymentFrequency", it.paymentFrequency)
                        put("linkedAccountId", it.linkedAccountId ?: JSONObject.NULL)
                        put("priority", it.priority)
                        put("notes", it.notes ?: JSONObject.NULL)
                        put("color", it.color)
                        put("icon", it.icon)
                        put("createdAt", it.createdAt)
                        put("isClosed", it.isClosed)
                    })
                }
                backupObj.put("debts", debtsArray)

                // Capture Debt Payments
                val debtPaymentsArray = JSONArray()
                database.debtPaymentDao().getAllPayments().first().forEach {
                    debtPaymentsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("debtId", it.debtId)
                        put("accountId", it.accountId)
                        put("amount", it.amount)
                        put("paymentDate", it.paymentDate)
                        put("paymentType", it.paymentType)
                        put("note", it.note ?: JSONObject.NULL)
                        put("linkedTransactionId", it.linkedTransactionId ?: JSONObject.NULL)
                        put("createdAt", it.createdAt)
                    })
                }
                backupObj.put("debt_payments", debtPaymentsArray)

                // Capture Transfers
                val transfersArray = JSONArray()
                database.transferDao().getAllTransfers().first().forEach {
                    transfersArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("fromAccountId", it.fromAccountId)
                        put("toAccountId", it.toAccountId)
                        put("amount", it.amount)
                        put("feeAmount", it.feeAmount ?: JSONObject.NULL)
                        put("note", it.note ?: JSONObject.NULL)
                        put("date", it.date)
                        put("referenceId", it.referenceId)
                        put("createdAt", it.createdAt)
                    })
                }
                backupObj.put("transfers", transfersArray)

                // Capture Budget Goals
                val budgetGoalsArray = JSONArray()
                database.budgetGoalDao().getAllBudgetGoals().first().forEach {
                    budgetGoalsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("title", it.title)
                        put("linkedCategoryId", it.linkedCategoryId ?: JSONObject.NULL)
                        put("budgetType", it.budgetType)
                        put("amountLimit", it.amountLimit)
                        put("spentAmount", it.spentAmount)
                        put("startDate", it.startDate)
                        put("endDate", it.endDate)
                        put("alertThresholdPercent", it.alertThresholdPercent)
                        put("isActive", it.isActive)
                        put("color", it.color)
                        put("icon", it.icon)
                        put("createdAt", it.createdAt)
                    })
                }
                backupObj.put("budget_goals", budgetGoalsArray)

                // Capture Financial Plans
                val plansArray = JSONArray()
                database.financialPlanDao().getAllPlans().first().forEach {
                    plansArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("title", it.title)
                        put("type", it.type)
                        put("targetAmount", it.targetAmount)
                        put("currentAmount", it.currentAmount)
                        put("linkedAccountIds", it.linkedAccountIds)
                        put("linkedCategoryIds", it.linkedCategoryIds)
                        put("startDate", it.startDate)
                        put("endDate", it.endDate ?: JSONObject.NULL)
                        put("status", it.status)
                        put("notes", it.notes ?: JSONObject.NULL)
                        put("color", it.color)
                        put("icon", it.icon)
                        put("createdAt", it.createdAt)
                    })
                }
                backupObj.put("financial_plans", plansArray)

                // Capture Transaction Templates
                val templatesArray = JSONArray()
                database.transactionTemplateDao().getAllTemplates().first().forEach {
                    templatesArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("amount", it.amount)
                        put("transactionType", it.transactionType)
                        put("accountId", it.accountId)
                        put("targetAccountId", it.targetAccountId ?: JSONObject.NULL)
                        put("categoryId", it.categoryId ?: JSONObject.NULL)
                        put("subcategoryId", it.subcategoryId ?: JSONObject.NULL)
                        put("notes", it.notes ?: JSONObject.NULL)
                        put("iconEmoji", it.iconEmoji ?: JSONObject.NULL)
                        put("colorHex", it.colorHex ?: JSONObject.NULL)
                        put("isPinned", it.isPinned)
                        put("usageCount", it.usageCount)
                        put("lastUsedAt", it.lastUsedAt ?: JSONObject.NULL)
                        put("createdAt", it.createdAt)
                        put("updatedAt", it.updatedAt)
                    })
                }
                backupObj.put("transaction_templates", templatesArray)

                // Capture Notifications
                val notificationsArray = JSONArray()
                database.notificationDao().getAllNotifications().first().forEach {
                    notificationsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("title", it.title)
                        put("message", it.message)
                        put("type", it.type)
                        put("isRead", it.isRead)
                        put("timestamp", it.timestamp)
                        put("deepLinkRoute", it.deepLinkRoute ?: JSONObject.NULL)
                        put("relatedEntityId", it.relatedEntityId ?: JSONObject.NULL)
                    })
                }
                backupObj.put("notifications", notificationsArray)

                // Save JSON to local backup file as a cached secure state (encrypted using AES-256)
                val backupFile = File(context.filesDir, "kdach_backup_drive.json")
                val encryptedData = com.example.core.utils.CryptoUtils.encrypt(backupObj.toString())
                backupFile.writeText(encryptedData)

                // Update settings metadata
                val dateString = FormatterUtils.formatDate(System.currentTimeMillis())
                val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("last_backup_date", dateString).apply()

                _uiState.update {
                    it.copy(
                        lastBackupDate = dateString,
                        isSyncing = false,
                        backupRestoreStatus = "تم النسخ الاحتياطي بنجاح كملف JSON آمن!"
                    )
                }
                onSuccess(dateString)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, backupRestoreStatus = "فشل النسخ الاحتياطي: ${e.localizedMessage}") }
                onFailure(e.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }

    // JSON Restore System: conflict-safe restore with confirmation
    fun runRestore(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري قراءة ملف الاستعادة...") }

        viewModelScope.launch {
            try {
                val backupFile = File(context.filesDir, "kdach_backup_drive.json")
                if (!backupFile.exists()) {
                    _uiState.update { it.copy(isSyncing = false, backupRestoreStatus = "لا توجد نسخة احتياطية محفوظة للاتصال!") }
                    onFailure("ملف النسخة الاحتياطية غير موجود.")
                    return@launch
                }

                val encryptedBackupStr = backupFile.readText()
                val backupStr = try {
                    com.example.core.utils.CryptoUtils.decrypt(encryptedBackupStr)
                } catch (e: Exception) {
                    // Fallback in case the file was not encrypted (old plain JSON backup compatibility)
                    if (encryptedBackupStr.trim().startsWith("{")) {
                        encryptedBackupStr
                    } else {
                        throw e
                    }
                }
                val backupObj = JSONObject(backupStr)

                // Schema validation: check that the JSON contains required tables before deleting anything
                if (!backupObj.has("accounts") || !backupObj.has("transactions") || !backupObj.has("categories")) {
                    throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح أو لا يحتوي على الجداول الأساسية للتطبيق!")
                }

                database.withTransaction {
                    // Clear existing tables
                    database.openHelper.writableDatabase.execSQL("DELETE FROM transactions")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM accounts")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE isSystem = 0")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM income_sources")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM saving_goals")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM savings_contributions")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM subscriptions")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM debts")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM debt_payments")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM transfers")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM budget_goals")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM financial_plans")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM transaction_templates")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM notifications")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM daily_financial_aggregates")

                    // 1. Restore Accounts
                    val accountsArray = backupObj.optJSONArray("accounts")
                    if (accountsArray != null) {
                        for (i in 0 until accountsArray.length()) {
                            val obj = accountsArray.getJSONObject(i)
                            database.accountDao().insertAccount(
                                AccountEntity(
                                    id = obj.getLong("id"),
                                    name = obj.getString("name"),
                                    type = obj.getString("type"),
                                    balance = obj.getDouble("balance"),
                                    currency = obj.optString("currency", "DZD"),
                                    color = obj.getString("color"),
                                    icon = obj.getString("icon"),
                                    isDefault = obj.optBoolean("isDefault", false),
                                    isArchived = obj.optBoolean("isArchived", false),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 2. Restore Categories (Only custom ones)
                    val catsArray = backupObj.optJSONArray("categories")
                    if (catsArray != null) {
                        for (i in 0 until catsArray.length()) {
                            val obj = catsArray.getJSONObject(i)
                            database.categoryDao().insertCategory(
                                CategoryEntity(
                                    id = obj.getLong("id"),
                                    name = obj.getString("name"),
                                    type = obj.getString("type"),
                                    icon = obj.getString("icon"),
                                    color = obj.getString("color"),
                                    budgetLimit = if (obj.isNull("budgetLimit")) null else obj.getDouble("budgetLimit"),
                                    isSystem = obj.optBoolean("isSystem", false),
                                    parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getLong("parentId") else null,
                                    sortOrder = obj.optInt("sortOrder", 0)
                                )
                            )
                        }
                    }

                    // 3. Restore Transactions (direct to DAO to prevent double-accounting logic)
                    val txArray = backupObj.optJSONArray("transactions")
                    if (txArray != null) {
                        for (i in 0 until txArray.length()) {
                            val obj = txArray.getJSONObject(i)
                            database.transactionDao().insertTransaction(
                                TransactionEntity(
                                    id = obj.getLong("id"),
                                    amount = obj.getDouble("amount"),
                                    type = obj.getString("type"),
                                    categoryId = obj.getLong("categoryId"),
                                    accountId = obj.getLong("accountId"),
                                    toAccountId = if (obj.has("toAccountId") && !obj.isNull("toAccountId")) obj.getLong("toAccountId") else null,
                                    note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                                    date = obj.getLong("date"),
                                    isRecurring = obj.optBoolean("isRecurring", false),
                                    recurringPeriod = if (obj.has("recurringPeriod") && !obj.isNull("recurringPeriod")) obj.getString("recurringPeriod") else null,
                                    attachmentPath = if (obj.has("attachmentPath") && !obj.isNull("attachmentPath")) obj.getString("attachmentPath") else null
                                )
                            )
                        }
                    }

                    // 4. Restore Income Sources
                    val incomeArray = backupObj.optJSONArray("income_sources")
                    if (incomeArray != null) {
                        for (i in 0 until incomeArray.length()) {
                            val obj = incomeArray.getJSONObject(i)
                            database.incomeSourceDao().insertIncomeSource(
                                IncomeSourceEntity(
                                    id = obj.getLong("id"),
                                    name = obj.getString("name"),
                                    amount = obj.getDouble("amount"),
                                    type = obj.getString("type"),
                                    accountId = obj.getLong("accountId"),
                                    dayOfMonth = obj.getInt("dayOfMonth"),
                                    isActive = obj.optBoolean("isActive", true),
                                    nextExpectedDate = obj.optLong("nextExpectedDate", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 5. Restore Saving Goals
                    val savingsGoalsArray = backupObj.optJSONArray("saving_goals")
                    if (savingsGoalsArray != null) {
                        for (i in 0 until savingsGoalsArray.length()) {
                            val obj = savingsGoalsArray.getJSONObject(i)
                            database.savingGoalDao().insertSavingGoal(
                                SavingGoalEntity(
                                    id = obj.getLong("id"),
                                    name = obj.getString("name"),
                                    targetAmount = obj.getDouble("targetAmount"),
                                    currentAmount = obj.optDouble("currentAmount", 0.0),
                                    deadline = if (obj.has("deadline") && !obj.isNull("deadline")) obj.getLong("deadline") else null,
                                    accountId = obj.getLong("accountId"),
                                    icon = obj.getString("icon"),
                                    color = obj.getString("color"),
                                    isCompleted = obj.optBoolean("isCompleted", false)
                                )
                            )
                        }
                    }

                    // 6. Restore Savings Contributions
                    val savingsContributionsArray = backupObj.optJSONArray("savings_contributions")
                    if (savingsContributionsArray != null) {
                        for (i in 0 until savingsContributionsArray.length()) {
                            val obj = savingsContributionsArray.getJSONObject(i)
                            database.savingsContributionDao().insertContribution(
                                SavingsContributionEntity(
                                    id = obj.getLong("id"),
                                    savingGoalId = obj.getLong("savingGoalId"),
                                    accountId = obj.getLong("accountId"),
                                    amount = obj.getDouble("amount"),
                                    type = obj.getString("type"),
                                    note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                                    date = obj.getLong("date"),
                                    linkedTransactionId = if (obj.has("linkedTransactionId") && !obj.isNull("linkedTransactionId")) obj.getLong("linkedTransactionId") else null,
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 7. Restore Subscriptions
                    val subscriptionsArray = backupObj.optJSONArray("subscriptions")
                    if (subscriptionsArray != null) {
                        for (i in 0 until subscriptionsArray.length()) {
                            val obj = subscriptionsArray.getJSONObject(i)
                            database.subscriptionDao().insertSubscription(
                                SubscriptionEntity(
                                    id = obj.getLong("id"),
                                    name = obj.getString("name"),
                                    amount = obj.getDouble("amount"),
                                    currency = obj.optString("currency", "DZD"),
                                    billingCycle = obj.getString("billingCycle"),
                                    nextBillingDate = obj.getLong("nextBillingDate"),
                                    accountId = obj.getLong("accountId"),
                                    categoryId = obj.getLong("categoryId"),
                                    icon = if (obj.has("icon") && !obj.isNull("icon")) obj.getString("icon") else null,
                                    isActive = obj.optBoolean("isActive", true),
                                    reminderDaysBefore = obj.optInt("reminderDaysBefore", 3)
                                )
                            )
                        }
                    }

                    // 8. Restore Debts
                    val debtsArray = backupObj.optJSONArray("debts")
                    if (debtsArray != null) {
                        for (i in 0 until debtsArray.length()) {
                            val obj = debtsArray.getJSONObject(i)
                            database.debtDao().insertDebt(
                                DebtEntity(
                                    id = obj.getLong("id"),
                                    title = obj.getString("title"),
                                    creditorName = obj.getString("creditorName"),
                                    totalAmount = obj.getDouble("totalAmount"),
                                    remainingAmount = obj.getDouble("remainingAmount"),
                                    interestRate = if (obj.has("interestRate") && !obj.isNull("interestRate")) obj.getDouble("interestRate") else null,
                                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null,
                                    minimumPayment = obj.getDouble("minimumPayment"),
                                    recommendedPayment = if (obj.has("recommendedPayment") && !obj.isNull("recommendedPayment")) obj.getDouble("recommendedPayment") else null,
                                    paymentFrequency = obj.getString("paymentFrequency"),
                                    linkedAccountId = if (obj.has("linkedAccountId") && !obj.isNull("linkedAccountId")) obj.getLong("linkedAccountId") else null,
                                    priority = obj.getInt("priority"),
                                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                                    color = obj.getString("color"),
                                    icon = obj.getString("icon"),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                    isClosed = obj.optBoolean("isClosed", false)
                                )
                            )
                        }
                    }

                    // 9. Restore Debt Payments
                    val debtPaymentsArray = backupObj.optJSONArray("debt_payments")
                    if (debtPaymentsArray != null) {
                        for (i in 0 until debtPaymentsArray.length()) {
                            val obj = debtPaymentsArray.getJSONObject(i)
                            database.debtPaymentDao().insertPayment(
                                DebtPaymentEntity(
                                    id = obj.getLong("id"),
                                    debtId = obj.getLong("debtId"),
                                    accountId = obj.getLong("accountId"),
                                    amount = obj.getDouble("amount"),
                                    paymentDate = obj.getLong("paymentDate"),
                                    paymentType = obj.getString("paymentType"),
                                    note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                                    linkedTransactionId = if (obj.has("linkedTransactionId") && !obj.isNull("linkedTransactionId")) obj.getLong("linkedTransactionId") else null,
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 10. Restore Transfers
                    val transfersArray = backupObj.optJSONArray("transfers")
                    if (transfersArray != null) {
                        for (i in 0 until transfersArray.length()) {
                            val obj = transfersArray.getJSONObject(i)
                            database.transferDao().insertTransfer(
                                TransferEntity(
                                    id = obj.getLong("id"),
                                    fromAccountId = obj.getLong("fromAccountId"),
                                    toAccountId = obj.getLong("toAccountId"),
                                    amount = obj.getDouble("amount"),
                                    feeAmount = if (obj.has("feeAmount") && !obj.isNull("feeAmount")) obj.getDouble("feeAmount") else null,
                                    note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                                    date = obj.getLong("date"),
                                    referenceId = obj.getString("referenceId"),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 11. Restore Budget Goals
                    val budgetGoalsArray = backupObj.optJSONArray("budget_goals")
                    if (budgetGoalsArray != null) {
                        for (i in 0 until budgetGoalsArray.length()) {
                            val obj = budgetGoalsArray.getJSONObject(i)
                            database.budgetGoalDao().insertBudgetGoal(
                                BudgetGoalEntity(
                                    id = obj.getLong("id"),
                                    title = obj.getString("title"),
                                    linkedCategoryId = if (obj.has("linkedCategoryId") && !obj.isNull("linkedCategoryId")) obj.getLong("linkedCategoryId") else null,
                                    budgetType = obj.getString("budgetType"),
                                    amountLimit = obj.getDouble("amountLimit"),
                                    spentAmount = obj.optDouble("spentAmount", 0.0),
                                    startDate = obj.getLong("startDate"),
                                    endDate = obj.getLong("endDate"),
                                    alertThresholdPercent = obj.optInt("alertThresholdPercent", 80),
                                    isActive = obj.optBoolean("isActive", true),
                                    color = obj.getString("color"),
                                    icon = obj.getString("icon"),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 12. Restore Financial Plans
                    val plansArray = backupObj.optJSONArray("financial_plans")
                    if (plansArray != null) {
                        for (i in 0 until plansArray.length()) {
                            val obj = plansArray.getJSONObject(i)
                            database.financialPlanDao().insertPlan(
                                FinancialPlanEntity(
                                    id = obj.getLong("id"),
                                    title = obj.getString("title"),
                                    type = obj.getString("type"),
                                    targetAmount = obj.getDouble("targetAmount"),
                                    currentAmount = obj.optDouble("currentAmount", 0.0),
                                    linkedAccountIds = obj.optString("linkedAccountIds", ""),
                                    linkedCategoryIds = obj.optString("linkedCategoryIds", ""),
                                    startDate = obj.getLong("startDate"),
                                    endDate = if (obj.has("endDate") && !obj.isNull("endDate")) obj.getLong("endDate") else null,
                                    status = obj.optString("status", "ACTIVE"),
                                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                                    color = obj.optString("color", "#6C63FF"),
                                    icon = obj.optString("icon", "flag"),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 13. Restore Transaction Templates
                    val templatesArray = backupObj.optJSONArray("transaction_templates")
                    if (templatesArray != null) {
                        for (i in 0 until templatesArray.length()) {
                            val obj = templatesArray.getJSONObject(i)
                            database.transactionTemplateDao().insertTemplate(
                                TransactionTemplateEntity(
                                    id = obj.getLong("id"),
                                    name = obj.getString("name"),
                                    amount = obj.getDouble("amount"),
                                    transactionType = obj.getString("transactionType"),
                                    accountId = obj.getLong("accountId"),
                                    targetAccountId = if (obj.has("targetAccountId") && !obj.isNull("targetAccountId")) obj.getLong("targetAccountId") else null,
                                    categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                                    subcategoryId = if (obj.has("subcategoryId") && !obj.isNull("subcategoryId")) obj.getLong("subcategoryId") else null,
                                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                                    iconEmoji = if (obj.has("iconEmoji") && !obj.isNull("iconEmoji")) obj.getString("iconEmoji") else null,
                                    colorHex = if (obj.has("colorHex") && !obj.isNull("colorHex")) obj.getString("colorHex") else null,
                                    isPinned = obj.optBoolean("isPinned", false),
                                    usageCount = obj.optInt("usageCount", 0),
                                    lastUsedAt = if (obj.has("lastUsedAt") && !obj.isNull("lastUsedAt")) obj.getLong("lastUsedAt") else null,
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    // 14. Restore Notifications
                    val notificationsArray = backupObj.optJSONArray("notifications")
                    if (notificationsArray != null) {
                        for (i in 0 until notificationsArray.length()) {
                            val obj = notificationsArray.getJSONObject(i)
                            database.notificationDao().insertNotification(
                                NotificationEntity(
                                    id = obj.getLong("id"),
                                    title = obj.getString("title"),
                                    message = obj.getString("message"),
                                    type = obj.getString("type"),
                                    isRead = obj.optBoolean("isRead", false),
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                    deepLinkRoute = if (obj.has("deepLinkRoute") && !obj.isNull("deepLinkRoute")) obj.getString("deepLinkRoute") else null,
                                    relatedEntityId = if (obj.has("relatedEntityId") && !obj.isNull("relatedEntityId")) obj.getLong("relatedEntityId") else null
                                )
                            )
                        }
                    }

                    // 15. Regenerate daily financial aggregates to keep dashboard stats fully synchronized
                    val allRestoreTxs = database.transactionDao().getTransactionsByDateRangeList(0L, Long.MAX_VALUE)
                    val groupedTxs = allRestoreTxs.groupBy {
                        val calendar = java.util.Calendar.getInstance().apply {
                            timeInMillis = it.date
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        calendar.timeInMillis
                    }
                    groupedTxs.forEach { (startOfDay, txs) ->
                        val totalExp = txs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                        val totalInc = txs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
                        val count = txs.size
                        val volume = totalExp + totalInc
                        val score = 0.4 * kotlin.math.ln(count.toDouble() + 1.0) + 0.6 * kotlin.math.ln(volume + 1.0)

                        database.dailyFinancialAggregateDao().upsertAggregate(
                            DailyFinancialAggregateEntity(
                                localDateTimestamp = startOfDay,
                                totalExpense = totalExp,
                                totalIncome = totalInc,
                                transactionCount = count,
                                netCashflow = totalInc - totalExp,
                                activityScore = score
                            )
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        backupRestoreStatus = "تم استعادة البيانات بالكامل بنجاح!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, backupRestoreStatus = "فشلت الاستعادة: ${e.localizedMessage}") }
                onFailure(e.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }
}
