package com.qdash.data.repository

import android.util.JsonReader
import android.util.JsonWriter
import androidx.room.withTransaction
import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.*
import com.qdash.domain.model.TransactionType
import com.qdash.domain.repository.BackupRepository
import kotlinx.coroutines.flow.first
import com.qdash.core.data.DatabaseSeeder
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import android.content.Context
import android.net.Uri
import com.qdash.core.preferences.PreferencesManager
import com.qdash.data.backup.BackupManager

class BackupRepositoryImpl(
    private val context: Context? = null,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager? = null,
    private val backupManager: BackupManager? = null
) : BackupRepository {

    // --- Legacy V1 Methods for backward compatibility ---

    override suspend fun exportAllDataAsJson(): JSONObject = database.withTransaction {
        val backupObj = JSONObject()

        // 1. Capture Accounts
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

        // 2. Capture Categories
        val catsArray = JSONArray()
        database.categoryDao().getAllCategories().first().forEach {
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
        backupObj.put("categories", catsArray)

        // 3. Capture Transactions
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
                put("occurredAt", it.occurredAt ?: JSONObject.NULL)
            })
        }
        backupObj.put("transactions", txArray)

        // Capture remaining basic elements for compatibility
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

        val debtsArray = JSONArray()
        database.debtDao().getAllDebts().first().forEach {
            debtsArray.put(JSONObject().apply {
                val debt = it.debt
                val details = it.installmentDetails
                put("id", debt.id)
                put("title", debt.title)
                put("creditorName", debt.creditorName)
                put("totalAmount", debt.totalAmount)
                put("remainingAmount", debt.remainingAmount)
                put("interestRate", details?.interestRate ?: JSONObject.NULL)
                put("dueDate", debt.dueDate ?: JSONObject.NULL)
                put("minimumPayment", details?.minimumPayment ?: 0.0)
                put("recommendedPayment", details?.recommendedPayment ?: JSONObject.NULL)
                put("paymentFrequency", details?.paymentFrequency ?: "NONE")
                put("linkedAccountId", debt.linkedAccountId ?: JSONObject.NULL)
                put("priority", details?.priority ?: 3)
                put("notes", debt.notes ?: JSONObject.NULL)
                put("color", debt.color)
                put("icon", debt.icon)
                put("createdAt", debt.createdAt)
                put("isClosed", debt.isClosed)
                put("debtType", debt.debtType)
            })
        }
        backupObj.put("debts", debtsArray)

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

        backupObj
    }

    override suspend fun restoreFromJson(json: JSONObject) = database.withTransaction {
        if (!json.has("accounts") || !json.has("transactions") || !json.has("categories")) {
            throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح أو لا يحتوي على الجداول الأساسية للتطبيق!")
        }

        // Clear existing tables
        database.openHelper.writableDatabase.execSQL("DELETE FROM transactions")
        database.openHelper.writableDatabase.execSQL("DELETE FROM accounts")
        database.openHelper.writableDatabase.execSQL("DELETE FROM categories")
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
        val accountsArray = json.optJSONArray("accounts")
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

        // 2. Restore Categories — root categories first, then subcategories
        // (self-referential FK: parentId must reference an already-inserted row)
        val catsArray = json.optJSONArray("categories")
        if (catsArray != null) {
            // Build list of all category objects
            val allCatObjects = (0 until catsArray.length()).map { catsArray.getJSONObject(it) }
            val rootCats = allCatObjects.filter { it.isNull("parentId") || !it.has("parentId") }
            val subCats = allCatObjects.filter { !it.isNull("parentId") && it.has("parentId") }

            // Insert root categories first (parentId == null), then subcategories
            for (obj in rootCats + subCats) {
                database.categoryDao().insertCategory(
                    CategoryEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        icon = obj.getString("icon"),
                        color = obj.getString("color"),
                        budgetLimit = if (obj.isNull("budgetLimit")) null else obj.optDouble("budgetLimit"),
                        isSystem = obj.optBoolean("isSystem", false),
                        parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getLong("parentId") else null,
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }
        }

        // 3. Restore Transactions
        val txArray = json.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        amount = obj.getDouble("amount"),
                        type = obj.getString("type"),
                        // categoryId is nullable — old backups may have 0 or missing value
                        categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) {
                            val cid = obj.getLong("categoryId")
                            if (cid == 0L) null else cid
                        } else null,
                        accountId = obj.getLong("accountId"),
                        toAccountId = if (obj.has("toAccountId") && !obj.isNull("toAccountId")) obj.getLong("toAccountId") else null,
                        note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                        date = obj.getLong("date"),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        recurringPeriod = if (obj.has("recurringPeriod") && !obj.isNull("recurringPeriod")) obj.getString("recurringPeriod") else null,
                        attachmentPath = if (obj.has("attachmentPath") && !obj.isNull("attachmentPath")) obj.getString("attachmentPath") else null,
                        tags = if (obj.has("tags") && !obj.isNull("tags")) obj.getString("tags") else null,
                        // kind/isDebit: provide safe defaults for very old backups
                        kind = obj.optString("kind", obj.optString("type", "INCOME")),
                        isDebit = obj.optBoolean("isDebit", true),
                        transferId = if (obj.has("transferId") && !obj.isNull("transferId")) obj.getString("transferId") else null,
                        // occurredAt: null for old backups (no fabricated time), preserved for new ones
                        occurredAt = if (obj.has("occurredAt") && !obj.isNull("occurredAt")) obj.getLong("occurredAt") else null
                    )
                )
            }
        }


        // Restore remaining tables
        val incomeArray = json.optJSONArray("income_sources")
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

        val savingsGoalsArray = json.optJSONArray("saving_goals")
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

        val savingsContributionsArray = json.optJSONArray("savings_contributions")
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

        val subscriptionsArray = json.optJSONArray("subscriptions")
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

        val debtsArray = json.optJSONArray("debts")
        if (debtsArray != null) {
            for (i in 0 until debtsArray.length()) {
                val obj = debtsArray.getJSONObject(i)
                val debtId = obj.getLong("id")
                val debtType = obj.optString("debtType", "INSTALLMENT")
                database.debtDao().insertDebt(
                    DebtEntity(
                        id = debtId,
                        title = obj.getString("title"),
                        creditorName = obj.getString("creditorName"),
                        totalAmount = obj.getDouble("totalAmount"),
                        remainingAmount = obj.getDouble("remainingAmount"),
                        dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null,
                        linkedAccountId = if (obj.has("linkedAccountId") && !obj.isNull("linkedAccountId")) obj.getLong("linkedAccountId") else null,
                        notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                        color = obj.getString("color"),
                        icon = obj.getString("icon"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isClosed = obj.optBoolean("isClosed", false),
                        debtType = debtType
                    )
                )
                if (debtType == "INSTALLMENT") {
                    database.debtDao().insertInstallmentDetails(
                        DebtInstallmentDetailsEntity(
                            debtId = debtId,
                            interestRate = if (obj.has("interestRate") && !obj.isNull("interestRate")) obj.getDouble("interestRate") else 0.0,
                            minimumPayment = obj.optDouble("minimumPayment", 0.0),
                            recommendedPayment = if (obj.has("recommendedPayment") && !obj.isNull("recommendedPayment")) obj.getDouble("recommendedPayment") else null,
                            paymentFrequency = obj.optString("paymentFrequency", "MONTHLY"),
                            priority = obj.optInt("priority", 3)
                        )
                    )
                }
            }
        }

        val debtPaymentsArray = json.optJSONArray("debt_payments")
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

        val transfersArray = json.optJSONArray("transfers")
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

        val budgetGoalsArray = json.optJSONArray("budget_goals")
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

        val plansArray = json.optJSONArray("financial_plans")
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

        val templatesArray = json.optJSONArray("transaction_templates")
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

        val notificationsArray = json.optJSONArray("notifications")
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

        regenerateDailyFinancialAggregates()
        DatabaseSeeder.prepopulateSystemDefaults(database)
    }

    private suspend fun regenerateDailyFinancialAggregates() {
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

    // --- New V2 Streaming APIs ---

    override suspend fun exportBackupV2(outputStream: OutputStream, selectedTables: List<String>?): Map<String, Int> = database.withTransaction {
        val counts = mutableMapOf<String, Int>()
        val writer = JsonWriter(BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")))
        writer.setIndent("  ")
        writer.beginObject() // {

        fun isSelected(table: String) = selectedTables == null || selectedTables.contains(table)

        // 1. Accounts
        if (isSelected("accounts")) {
            val list = database.accountDao().getAllAccountsIncludingArchived().first()
            counts["accounts"] = list.size
            writer.name("accounts").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("name").value(it.name)
                writer.name("type").value(it.type)
                writer.name("balance").value(it.balance)
                writer.name("currency").value(it.currency)
                writer.name("color").value(it.color)
                writer.name("icon").value(it.icon)
                writer.name("isDefault").value(it.isDefault)
                writer.name("isArchived").value(it.isArchived)
                writer.name("createdAt").value(it.createdAt)
                writer.name("sortOrder").value(it.sortOrder)
                writer.endObject()
            }
            writer.endArray()
        }

        // 2. Categories
        if (isSelected("categories")) {
            val list = database.categoryDao().getAllCategories().first()
            counts["categories"] = list.size
            writer.name("categories").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("name").value(it.name)
                writer.name("type").value(it.type)
                writer.name("icon").value(it.icon)
                writer.name("color").value(it.color)
                writer.name("budgetLimit")
                if (it.budgetLimit == null) writer.nullValue() else writer.value(it.budgetLimit)
                writer.name("isSystem").value(it.isSystem)
                writer.name("parentId")
                if (it.parentId == null) writer.nullValue() else writer.value(it.parentId)
                writer.name("sortOrder").value(it.sortOrder)
                writer.endObject()
            }
            writer.endArray()
        }

        // 3. Transactions
        if (isSelected("transactions")) {
            val list = database.transactionDao().getAllTransactions().first()
            counts["transactions"] = list.size
            writer.name("transactions").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("amount").value(it.amount)
                writer.name("type").value(it.type)
                writer.name("categoryId")
                if (it.categoryId == null) writer.nullValue() else writer.value(it.categoryId)
                writer.name("accountId").value(it.accountId)
                writer.name("toAccountId")
                if (it.toAccountId == null) writer.nullValue() else writer.value(it.toAccountId)
                writer.name("note")
                if (it.note == null) writer.nullValue() else writer.value(it.note)
                writer.name("date").value(it.date)
                writer.name("isRecurring").value(it.isRecurring)
                writer.name("recurringPeriod")
                if (it.recurringPeriod == null) writer.nullValue() else writer.value(it.recurringPeriod)
                writer.name("attachmentPath")
                if (it.attachmentPath == null) writer.nullValue() else writer.value(it.attachmentPath)
                writer.name("tags")
                if (it.tags == null) writer.nullValue() else writer.value(it.tags)
                writer.name("suggestedCategoryId")
                if (it.suggestedCategoryId == null) writer.nullValue() else writer.value(it.suggestedCategoryId)
                writer.name("suggestionSource")
                if (it.suggestionSource == null) writer.nullValue() else writer.value(it.suggestionSource)
                writer.name("confidenceScore")
                if (it.confidenceScore == null) writer.nullValue() else writer.value(it.confidenceScore.toDouble())
                writer.name("userAcceptedSuggestion")
                if (it.userAcceptedSuggestion == null) writer.nullValue() else writer.value(it.userAcceptedSuggestion)
                writer.name("kind").value(it.kind)
                writer.name("transferId")
                if (it.transferId == null) writer.nullValue() else writer.value(it.transferId)
                writer.name("isDebit").value(it.isDebit)
                writer.endObject()
            }
            writer.endArray()
        }

        // 4. Income Sources
        if (isSelected("income_sources")) {
            val list = database.incomeSourceDao().getAllIncomeSources().first()
            counts["income_sources"] = list.size
            writer.name("income_sources").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("name").value(it.name)
                writer.name("amount").value(it.amount)
                writer.name("type").value(it.type)
                writer.name("accountId").value(it.accountId)
                writer.name("dayOfMonth").value(it.dayOfMonth.toLong())
                writer.name("isActive").value(it.isActive)
                writer.name("nextExpectedDate").value(it.nextExpectedDate)
                writer.endObject()
            }
            writer.endArray()
        }

        // 5. Saving Goals
        if (isSelected("saving_goals")) {
            val list = database.savingGoalDao().getAllSavingGoals().first()
            counts["saving_goals"] = list.size
            writer.name("saving_goals").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("name").value(it.name)
                writer.name("targetAmount").value(it.targetAmount)
                writer.name("currentAmount").value(it.currentAmount)
                writer.name("deadline")
                if (it.deadline == null) writer.nullValue() else writer.value(it.deadline)
                writer.name("accountId").value(it.accountId)
                writer.name("icon").value(it.icon)
                writer.name("color").value(it.color)
                writer.name("isCompleted").value(it.isCompleted)
                writer.endObject()
            }
            writer.endArray()
        }

        // 6. Savings Contributions
        if (isSelected("savings_contributions")) {
            val list = database.savingsContributionDao().getAllContributions().first()
            counts["savings_contributions"] = list.size
            writer.name("savings_contributions").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("savingGoalId").value(it.savingGoalId)
                writer.name("accountId").value(it.accountId)
                writer.name("amount").value(it.amount)
                writer.name("type").value(it.type)
                writer.name("note")
                if (it.note == null) writer.nullValue() else writer.value(it.note)
                writer.name("date").value(it.date)
                writer.name("linkedTransactionId")
                if (it.linkedTransactionId == null) writer.nullValue() else writer.value(it.linkedTransactionId)
                writer.name("createdAt").value(it.createdAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 7. Subscriptions
        if (isSelected("subscriptions")) {
            val list = database.subscriptionDao().getAllSubscriptions().first()
            counts["subscriptions"] = list.size
            writer.name("subscriptions").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("name").value(it.name)
                writer.name("amount").value(it.amount)
                writer.name("currency").value(it.currency)
                writer.name("billingCycle").value(it.billingCycle)
                writer.name("nextBillingDate").value(it.nextBillingDate)
                writer.name("accountId").value(it.accountId)
                writer.name("categoryId").value(it.categoryId)
                writer.name("icon")
                if (it.icon == null) writer.nullValue() else writer.value(it.icon)
                writer.name("isActive").value(it.isActive)
                writer.name("reminderDaysBefore").value(it.reminderDaysBefore.toLong())
                writer.name("isAutoShiftableBySalary").value(it.isAutoShiftableBySalary)
                writer.endObject()
            }
            writer.endArray()
        }

        // 8. Debts
        if (isSelected("debts")) {
            val list = database.debtDao().getAllDebts().first()
            counts["debts"] = list.size
            writer.name("debts").beginArray()
            list.forEach {
                val debt = it.debt
                val details = it.installmentDetails
                writer.beginObject()
                writer.name("id").value(debt.id)
                writer.name("title").value(debt.title)
                writer.name("creditorName").value(debt.creditorName)
                writer.name("totalAmount").value(debt.totalAmount)
                writer.name("remainingAmount").value(debt.remainingAmount)
                writer.name("interestRate")
                if (details?.interestRate == null) writer.nullValue() else writer.value(details.interestRate)
                writer.name("dueDate")
                if (debt.dueDate == null) writer.nullValue() else writer.value(debt.dueDate)
                writer.name("minimumPayment").value(details?.minimumPayment ?: 0.0)
                writer.name("recommendedPayment")
                if (details?.recommendedPayment == null) writer.nullValue() else writer.value(details.recommendedPayment)
                writer.name("paymentFrequency").value(details?.paymentFrequency ?: "MONTHLY")
                writer.name("linkedAccountId")
                if (debt.linkedAccountId == null) writer.nullValue() else writer.value(debt.linkedAccountId)
                writer.name("priority").value((details?.priority ?: 3).toLong())
                writer.name("notes")
                if (debt.notes == null) writer.nullValue() else writer.value(debt.notes)
                writer.name("color").value(debt.color)
                writer.name("icon").value(debt.icon)
                writer.name("createdAt").value(debt.createdAt)
                writer.name("isClosed").value(debt.isClosed)
                writer.name("debtType").value(debt.debtType)
                writer.endObject()
            }
            writer.endArray()
        }

        // 9. Debt Payments
        if (isSelected("debt_payments")) {
            val list = database.debtPaymentDao().getAllPayments().first()
            counts["debt_payments"] = list.size
            writer.name("debt_payments").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("debtId").value(it.debtId)
                writer.name("accountId").value(it.accountId)
                writer.name("amount").value(it.amount)
                writer.name("paymentDate").value(it.paymentDate)
                writer.name("paymentType").value(it.paymentType)
                writer.name("note")
                if (it.note == null) writer.nullValue() else writer.value(it.note)
                writer.name("linkedTransactionId")
                if (it.linkedTransactionId == null) writer.nullValue() else writer.value(it.linkedTransactionId)
                writer.name("createdAt").value(it.createdAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 10. Transfers
        if (isSelected("transfers")) {
            val list = database.transferDao().getAllTransfers().first()
            counts["transfers"] = list.size
            writer.name("transfers").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("fromAccountId").value(it.fromAccountId)
                writer.name("toAccountId").value(it.toAccountId)
                writer.name("amount").value(it.amount)
                writer.name("feeAmount")
                if (it.feeAmount == null) writer.nullValue() else writer.value(it.feeAmount)
                writer.name("note")
                if (it.note == null) writer.nullValue() else writer.value(it.note)
                writer.name("date").value(it.date)
                writer.name("referenceId").value(it.referenceId)
                writer.name("createdAt").value(it.createdAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 11. Budget Goals
        if (isSelected("budget_goals")) {
            val list = database.budgetGoalDao().getAllBudgetGoals().first()
            counts["budget_goals"] = list.size
            writer.name("budget_goals").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("title").value(it.title)
                writer.name("linkedCategoryId")
                if (it.linkedCategoryId == null) writer.nullValue() else writer.value(it.linkedCategoryId)
                writer.name("budgetType").value(it.budgetType)
                writer.name("amountLimit").value(it.amountLimit)
                writer.name("spentAmount").value(it.spentAmount)
                writer.name("startDate").value(it.startDate)
                writer.name("endDate").value(it.endDate)
                writer.name("alertThresholdPercent").value(it.alertThresholdPercent.toLong())
                writer.name("isActive").value(it.isActive)
                writer.name("color").value(it.color)
                writer.name("icon").value(it.icon)
                writer.name("createdAt").value(it.createdAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 12. Financial Plans
        if (isSelected("financial_plans")) {
            val list = database.financialPlanDao().getAllPlans().first()
            counts["financial_plans"] = list.size
            writer.name("financial_plans").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("title").value(it.title)
                writer.name("type").value(it.type)
                writer.name("targetAmount").value(it.targetAmount)
                writer.name("currentAmount").value(it.currentAmount)
                writer.name("linkedAccountIds").value(it.linkedAccountIds)
                writer.name("linkedCategoryIds").value(it.linkedCategoryIds)
                writer.name("startDate").value(it.startDate)
                writer.name("endDate")
                if (it.endDate == null) writer.nullValue() else writer.value(it.endDate)
                writer.name("status").value(it.status)
                writer.name("notes")
                if (it.notes == null) writer.nullValue() else writer.value(it.notes)
                writer.name("color").value(it.color)
                writer.name("icon").value(it.icon)
                writer.name("createdAt").value(it.createdAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 13. Transaction Templates
        if (isSelected("transaction_templates")) {
            val list = database.transactionTemplateDao().getAllTemplates().first()
            counts["transaction_templates"] = list.size
            writer.name("transaction_templates").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("name").value(it.name)
                writer.name("amount").value(it.amount)
                writer.name("transactionType").value(it.transactionType)
                writer.name("accountId").value(it.accountId)
                writer.name("targetAccountId")
                if (it.targetAccountId == null) writer.nullValue() else writer.value(it.targetAccountId)
                writer.name("categoryId")
                if (it.categoryId == null) writer.nullValue() else writer.value(it.categoryId)
                writer.name("subcategoryId")
                if (it.subcategoryId == null) writer.nullValue() else writer.value(it.subcategoryId)
                writer.name("notes")
                if (it.notes == null) writer.nullValue() else writer.value(it.notes)
                writer.name("iconEmoji")
                if (it.iconEmoji == null) writer.nullValue() else writer.value(it.iconEmoji)
                writer.name("colorHex")
                if (it.colorHex == null) writer.nullValue() else writer.value(it.colorHex)
                writer.name("isPinned").value(it.isPinned)
                writer.name("usageCount").value(it.usageCount.toLong())
                writer.name("lastUsedAt")
                if (it.lastUsedAt == null) writer.nullValue() else writer.value(it.lastUsedAt)
                writer.name("createdAt").value(it.createdAt)
                writer.name("updatedAt").value(it.updatedAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 14. Notifications
        if (isSelected("notifications")) {
            val list = database.notificationDao().getAllNotifications().first()
            counts["notifications"] = list.size
            writer.name("notifications").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("title").value(it.title)
                writer.name("message").value(it.message)
                writer.name("type").value(it.type)
                writer.name("isRead").value(it.isRead)
                writer.name("timestamp").value(it.timestamp)
                writer.name("deepLinkRoute")
                if (it.deepLinkRoute == null) writer.nullValue() else writer.value(it.deepLinkRoute)
                writer.name("relatedEntityId")
                if (it.relatedEntityId == null) writer.nullValue() else writer.value(it.relatedEntityId)
                writer.endObject()
            }
            writer.endArray()
        }

        // 15. Category Rules
        if (isSelected("category_rules")) {
            val list = database.categoryRuleDao().getAllRules().first()
            counts["category_rules"] = list.size
            writer.name("category_rules").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("keyword").value(it.keyword)
                writer.name("categoryId").value(it.categoryId)
                writer.name("priority").value(it.priority.toLong())
                writer.name("source").value(it.source)
                writer.name("isActive").value(it.isActive)
                writer.endObject()
            }
            writer.endArray()
        }

        // 16. User Category Mappings
        if (isSelected("user_category_mappings")) {
            val list = database.userCategoryMappingDao().getAllMappings().first()
            counts["user_category_mappings"] = list.size
            writer.name("user_category_mappings").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("normalizedText").value(it.normalizedText)
                writer.name("categoryId").value(it.categoryId)
                writer.name("usageCount").value(it.usageCount.toLong())
                writer.name("lastUsedAt").value(it.lastUsedAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 17. AI Chat Messages
        if (isSelected("ai_chat_messages")) {
            val list = database.aiChatDao().getAllMessagesOnce()
            counts["ai_chat_messages"] = list.size
            writer.name("ai_chat_messages").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("sender").value(it.sender)
                writer.name("message").value(it.message)
                writer.name("timestamp").value(it.timestamp)
                writer.name("sessionTitle").value(it.sessionTitle)
                writer.endObject()
            }
            writer.endArray()
        }

        // 18. Postal Profiles
        if (isSelected("postal_profiles")) {
            val list = database.postalProfileDao().getAllProfiles().first()
            counts["postal_profiles"] = list.size
            writer.name("postal_profiles").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("profileName").value(it.profileName)
                writer.name("firstName").value(it.firstName)
                writer.name("lastName").value(it.lastName)
                writer.name("fullName").value(it.fullName)
                writer.name("accountNumber").value(it.accountNumber)
                writer.name("accountKey").value(it.accountKey)
                writer.name("phone")
                if (it.phone == null) writer.nullValue() else writer.value(it.phone)
                writer.name("address")
                if (it.address == null) writer.nullValue() else writer.value(it.address)
                writer.name("city")
                if (it.city == null) writer.nullValue() else writer.value(it.city)
                writer.name("defaultRole").value(it.defaultRole)
                writer.name("isFavorite").value(it.isFavorite)
                writer.name("createdAt").value(it.createdAt)
                writer.name("updatedAt").value(it.updatedAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 19. Salary Delays
        if (isSelected("salary_delays")) {
            val list = database.salaryDelayDao().getAllSalaryDelaysOnce()
            counts["salary_delays"] = list.size
            writer.name("salary_delays").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("salaryId").value(it.salaryId)
                writer.name("delayDays").value(it.delayDays.toLong())
                writer.name("originalDate").value(it.originalDate)
                writer.name("newDate").value(it.newDate)
                writer.name("severityScore").value(it.severityScore.toLong())
                writer.name("status").value(it.status)
                writer.name("createdAt").value(it.createdAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 20. Salary Distributions
        if (isSelected("salary_distributions")) {
            val list = database.salaryDistributionDao().getAllDistributionsOnce()
            counts["salary_distributions"] = list.size
            writer.name("salary_distributions").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("salaryId").value(it.salaryId)
                writer.name("isEnabled").value(it.isEnabled)
                writer.name("needsPercentage").value(it.needsPercentage.toLong())
                writer.name("wantsPercentage").value(it.wantsPercentage.toLong())
                writer.name("savingsPercentage").value(it.savingsPercentage.toLong())
                writer.name("createdAt").value(it.createdAt)
                writer.name("updatedAt").value(it.updatedAt)
                writer.endObject()
            }
            writer.endArray()
        }

        // 21. Salary Envelopes
        if (isSelected("salary_envelopes")) {
            val list = database.salaryDistributionDao().getAllEnvelopesOnce()
            counts["salary_envelopes"] = list.size
            writer.name("salary_envelopes").beginArray()
            list.forEach {
                writer.beginObject()
                writer.name("id").value(it.id)
                writer.name("distributionId").value(it.distributionId)
                writer.name("type").value(it.type)
                writer.name("label").value(it.label)
                writer.name("percentage").value(it.percentage.toLong())
                writer.name("allocatedAmount").value(it.allocatedAmount)
                writer.name("spentAmount").value(it.spentAmount)
                writer.name("linkedCategoryIds").value(it.linkedCategoryIds)
                writer.name("linkedAccountId")
                if (it.linkedAccountId == null) writer.nullValue() else writer.value(it.linkedAccountId)
                writer.name("color").value(it.color)
                writer.name("icon").value(it.icon)
                writer.endObject()
            }
            writer.endArray()
        }

        writer.endObject() // }
        writer.close()
        counts
    }

    override suspend fun restoreBackupV2(inputStream: InputStream, selectedTables: List<String>?) = database.withTransaction {
        val reader = JsonReader(BufferedReader(InputStreamReader(inputStream, "UTF-8")))

        fun isSelected(table: String) = selectedTables == null || selectedTables.contains(table)

        // ── Pass 1: collect all table data into memory ─────────────────────────
        // We read the entire JSON first so that we can insert in FK dependency
        // order regardless of the order the keys appear in the backup file.
        val tableData = mutableMapOf<String, MutableList<Map<String, Any?>>>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val rows = mutableListOf<Map<String, Any?>>()
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                val fields = mutableMapOf<String, Any?>()
                while (reader.hasNext()) {
                    val fieldName = reader.nextName()
                    if (reader.peek() == android.util.JsonToken.NULL) {
                        reader.nextNull()
                        fields[fieldName] = null
                    } else {
                        when (reader.peek()) {
                            android.util.JsonToken.BOOLEAN -> fields[fieldName] = reader.nextBoolean()
                            android.util.JsonToken.NUMBER -> {
                                val valueStr = reader.nextString()
                                fields[fieldName] = valueStr.toDoubleOrNull() ?: valueStr.toLongOrNull() ?: valueStr
                            }
                            else -> fields[fieldName] = reader.nextString()
                        }
                    }
                }
                reader.endObject()
                rows.add(fields)
            }
            reader.endArray()
            tableData[key] = rows
        }
        reader.endObject()
        reader.close()

        // ── Clear only selected tables ──────────────────────────────────────────
        // Delete in reverse-dependency order so CASCADE actions don't interfere.
        if (isSelected("salary_envelopes")) database.openHelper.writableDatabase.execSQL("DELETE FROM salary_envelopes")
        if (isSelected("salary_distributions")) database.openHelper.writableDatabase.execSQL("DELETE FROM salary_distributions")
        if (isSelected("salary_delays")) database.openHelper.writableDatabase.execSQL("DELETE FROM salary_delays")
        if (isSelected("postal_profiles")) database.openHelper.writableDatabase.execSQL("DELETE FROM postal_profiles")
        if (isSelected("ai_chat_messages")) database.openHelper.writableDatabase.execSQL("DELETE FROM ai_chat_messages")
        if (isSelected("user_category_mappings")) database.openHelper.writableDatabase.execSQL("DELETE FROM user_category_mappings")
        if (isSelected("category_rules")) database.openHelper.writableDatabase.execSQL("DELETE FROM category_rules")
        if (isSelected("notifications")) database.openHelper.writableDatabase.execSQL("DELETE FROM notifications")
        if (isSelected("transaction_templates")) database.openHelper.writableDatabase.execSQL("DELETE FROM transaction_templates")
        if (isSelected("financial_plans")) database.openHelper.writableDatabase.execSQL("DELETE FROM financial_plans")
        if (isSelected("budget_goals")) database.openHelper.writableDatabase.execSQL("DELETE FROM budget_goals")
        if (isSelected("transfers")) database.openHelper.writableDatabase.execSQL("DELETE FROM transfers")
        if (isSelected("debt_payments")) database.openHelper.writableDatabase.execSQL("DELETE FROM debt_payments")
        if (isSelected("debts")) database.openHelper.writableDatabase.execSQL("DELETE FROM debts")
        if (isSelected("subscriptions")) database.openHelper.writableDatabase.execSQL("DELETE FROM subscriptions")
        if (isSelected("savings_contributions")) database.openHelper.writableDatabase.execSQL("DELETE FROM savings_contributions")
        if (isSelected("saving_goals")) database.openHelper.writableDatabase.execSQL("DELETE FROM saving_goals")
        if (isSelected("income_sources")) database.openHelper.writableDatabase.execSQL("DELETE FROM income_sources")
        if (isSelected("transactions")) database.openHelper.writableDatabase.execSQL("DELETE FROM transactions")
        if (isSelected("categories")) database.openHelper.writableDatabase.execSQL("DELETE FROM categories")
        if (isSelected("accounts")) database.openHelper.writableDatabase.execSQL("DELETE FROM accounts")
        if (isSelected("transactions")) database.openHelper.writableDatabase.execSQL("DELETE FROM daily_financial_aggregates")

        // ── Pass 2: insert in FK dependency order ───────────────────────────────
        // Tier 0 — no FK parents
        if (isSelected("accounts")) {
            tableData["accounts"]?.forEach { insertRow("accounts", it) }
        }

        // Tier 1 — categories: root categories first (parentId == null), then subcategories
        if (isSelected("categories")) {
            val categoryRows = tableData["categories"] ?: emptyList()
            val rootCategories = categoryRows.filter { it["parentId"] == null }
            val subCategories = categoryRows.filter { it["parentId"] != null }
            rootCategories.forEach { insertRow("categories", it) }
            subCategories.forEach { insertRow("categories", it) }
        }

        // Tier 2 — depend on accounts and/or root categories
        if (isSelected("income_sources")) {
            tableData["income_sources"]?.forEach { insertRow("income_sources", it) }
        }
        if (isSelected("saving_goals")) {
            tableData["saving_goals"]?.forEach { insertRow("saving_goals", it) }
        }
        if (isSelected("transactions")) {
            tableData["transactions"]?.forEach { insertRow("transactions", it) }
        }
        if (isSelected("subscriptions")) {
            tableData["subscriptions"]?.forEach { insertRow("subscriptions", it) }
        }
        if (isSelected("debts")) {
            tableData["debts"]?.forEach { insertRow("debts", it) }
        }
        if (isSelected("transfers")) {
            tableData["transfers"]?.forEach { insertRow("transfers", it) }
        }

        // Tier 3 — depend on saving_goals or debts (which must exist first)
        if (isSelected("savings_contributions")) {
            tableData["savings_contributions"]?.forEach { insertRow("savings_contributions", it) }
        }
        if (isSelected("debt_payments")) {
            tableData["debt_payments"]?.forEach { insertRow("debt_payments", it) }
        }

        // Tier 4 — no critical FK parents, or parents already inserted above
        if (isSelected("budget_goals")) {
            tableData["budget_goals"]?.forEach { insertRow("budget_goals", it) }
        }
        if (isSelected("financial_plans")) {
            tableData["financial_plans"]?.forEach { insertRow("financial_plans", it) }
        }
        if (isSelected("transaction_templates")) {
            tableData["transaction_templates"]?.forEach { insertRow("transaction_templates", it) }
        }
        if (isSelected("notifications")) {
            tableData["notifications"]?.forEach { insertRow("notifications", it) }
        }
        if (isSelected("category_rules")) {
            tableData["category_rules"]?.forEach { insertRow("category_rules", it) }
        }
        if (isSelected("user_category_mappings")) {
            tableData["user_category_mappings"]?.forEach { insertRow("user_category_mappings", it) }
        }
        if (isSelected("ai_chat_messages")) {
            tableData["ai_chat_messages"]?.forEach { insertRow("ai_chat_messages", it) }
        }
        if (isSelected("postal_profiles")) {
            tableData["postal_profiles"]?.forEach { insertRow("postal_profiles", it) }
        }
        if (isSelected("salary_delays")) {
            tableData["salary_delays"]?.forEach { insertRow("salary_delays", it) }
        }
        if (isSelected("salary_distributions")) {
            tableData["salary_distributions"]?.forEach { insertRow("salary_distributions", it) }
        }
        if (isSelected("salary_envelopes")) {
            tableData["salary_envelopes"]?.forEach { insertRow("salary_envelopes", it) }
        }

        // ── Post-restore tasks ──────────────────────────────────────────────────
        if (isSelected("transactions")) {
            regenerateDailyFinancialAggregates()
        }
        if (isSelected("categories")) {
            DatabaseSeeder.prepopulateSystemDefaults(database)
        }

        // FK integrity check — throws if any orphan records remain
        verifyRestoreIntegrity()
    }

    /**
     * Runs SQLite's built-in FK integrity check within the current transaction.
     * Throws [IllegalStateException] if any orphan (FK-violating) records are found,
     * which causes the surrounding withTransaction block to roll back automatically.
     */
    private fun verifyRestoreIntegrity() {
        val db = database.openHelper.writableDatabase
        // SupportSQLiteDatabase uses query() not rawQuery(); bindArgs must be an explicit typed array
        db.query("PRAGMA foreign_key_check", arrayOfNulls<Any>(0)).use { cursor ->
            if (cursor.moveToFirst()) {
                val table = cursor.getString(0)
                val rowId = cursor.getLong(1)
                val parent = cursor.getString(2)
                throw IllegalStateException(
                    "فشل فحص سلامة قاعدة البيانات: سجل يتيم في جدول '$table' (rowId=$rowId) يشير إلى '$parent' غير موجود. " +
                    "يرجى التحقق من سلامة ملف النسخة الاحتياطية."
                )
            }
        }
    }

    private suspend fun insertRow(table: String, f: Map<String, Any?>) {
        // Cast helper
        fun longVal(k: String) = (f[k] as? Number)?.toLong() ?: 0L
        fun longValOrNull(k: String) = (f[k] as? Number)?.toLong()
        fun doubleVal(k: String) = (f[k] as? Number)?.toDouble() ?: 0.0
        fun doubleValOrNull(k: String) = (f[k] as? Number)?.toDouble()
        fun floatValOrNull(k: String) = (f[k] as? Number)?.toFloat()
        fun stringVal(k: String) = f[k]?.toString() ?: ""
        fun stringValOrNull(k: String) = f[k]?.toString()
        fun boolVal(k: String) = f[k] as? Boolean ?: false
        fun boolValOrNull(k: String) = f[k] as? Boolean
        fun intVal(k: String) = (f[k] as? Number)?.toInt() ?: 0

        when (table) {
            "accounts" -> database.accountDao().insertAccount(
                AccountEntity(
                    id = longVal("id"),
                    name = stringVal("name"),
                    type = stringVal("type"),
                    balance = doubleVal("balance"),
                    currency = stringVal("currency").ifEmpty { "DZD" },
                    color = stringVal("color"),
                    icon = stringVal("icon"),
                    isDefault = boolVal("isDefault"),
                    isArchived = boolVal("isArchived"),
                    createdAt = longVal("createdAt"),
                    sortOrder = intVal("sortOrder")
                )
            )
            "categories" -> database.categoryDao().insertCategory(
                CategoryEntity(
                    id = longVal("id"),
                    name = stringVal("name"),
                    type = stringVal("type"),
                    icon = stringVal("icon"),
                    color = stringVal("color"),
                    budgetLimit = doubleValOrNull("budgetLimit"),
                    isSystem = boolVal("isSystem"),
                    parentId = longValOrNull("parentId"),
                    sortOrder = intVal("sortOrder")
                )
            )
            "transactions" -> database.transactionDao().insertTransaction(
                TransactionEntity(
                    id = longVal("id"),
                    amount = doubleVal("amount"),
                    type = stringVal("type"),
                    categoryId = longValOrNull("categoryId"),
                    accountId = longVal("accountId"),
                    toAccountId = longValOrNull("toAccountId"),
                    note = stringValOrNull("note"),
                    date = longVal("date"),
                    isRecurring = boolVal("isRecurring"),
                    recurringPeriod = stringValOrNull("recurringPeriod"),
                    attachmentPath = stringValOrNull("attachmentPath"),
                    tags = stringValOrNull("tags"),
                    suggestedCategoryId = longValOrNull("suggestedCategoryId"),
                    suggestionSource = stringValOrNull("suggestionSource"),
                    confidenceScore = floatValOrNull("confidenceScore"),
                    userAcceptedSuggestion = boolValOrNull("userAcceptedSuggestion"),
                    kind = stringVal("kind").ifEmpty { "INCOME" },
                    transferId = stringValOrNull("transferId"),
                    isDebit = boolVal("isDebit")
                )
            )
            "income_sources" -> database.incomeSourceDao().insertIncomeSource(
                IncomeSourceEntity(
                    id = longVal("id"),
                    name = stringVal("name"),
                    amount = doubleVal("amount"),
                    type = stringVal("type"),
                    accountId = longVal("accountId"),
                    dayOfMonth = intVal("dayOfMonth"),
                    isActive = boolVal("isActive"),
                    nextExpectedDate = longVal("nextExpectedDate")
                )
            )
            "saving_goals" -> database.savingGoalDao().insertSavingGoal(
                SavingGoalEntity(
                    id = longVal("id"),
                    name = stringVal("name"),
                    targetAmount = doubleVal("targetAmount"),
                    currentAmount = doubleVal("currentAmount"),
                    deadline = longValOrNull("deadline"),
                    accountId = longVal("accountId"),
                    icon = stringVal("icon"),
                    color = stringVal("color"),
                    isCompleted = boolVal("isCompleted")
                )
            )
            "savings_contributions" -> database.savingsContributionDao().insertContribution(
                SavingsContributionEntity(
                    id = longVal("id"),
                    savingGoalId = longVal("savingGoalId"),
                    accountId = longVal("accountId"),
                    amount = doubleVal("amount"),
                    type = stringVal("type"),
                    note = stringValOrNull("note"),
                    date = longVal("date"),
                    linkedTransactionId = longValOrNull("linkedTransactionId"),
                    createdAt = longVal("createdAt")
                )
            )
            "subscriptions" -> database.subscriptionDao().insertSubscription(
                SubscriptionEntity(
                    id = longVal("id"),
                    name = stringVal("name"),
                    amount = doubleVal("amount"),
                    currency = stringVal("currency").ifEmpty { "DZD" },
                    billingCycle = stringVal("billingCycle"),
                    nextBillingDate = longVal("nextBillingDate"),
                    accountId = longVal("accountId"),
                    categoryId = longVal("categoryId"),
                    icon = stringValOrNull("icon"),
                    isActive = boolVal("isActive"),
                    reminderDaysBefore = intVal("reminderDaysBefore"),
                    isAutoShiftableBySalary = boolVal("isAutoShiftableBySalary")
                )
            )
            "debts" -> {
                val debtId = longVal("id")
                val dType = stringVal("debtType").ifEmpty { "INSTALLMENT" }
                database.debtDao().insertDebt(
                    DebtEntity(
                        id = debtId,
                        title = stringVal("title"),
                        creditorName = stringVal("creditorName"),
                        totalAmount = doubleVal("totalAmount"),
                        remainingAmount = doubleVal("remainingAmount"),
                        dueDate = longValOrNull("dueDate"),
                        linkedAccountId = longValOrNull("linkedAccountId"),
                        notes = stringValOrNull("notes"),
                        color = stringVal("color"),
                        icon = stringVal("icon"),
                        createdAt = longVal("createdAt"),
                        isClosed = boolVal("isClosed"),
                        debtType = dType
                    )
                )
                if (dType == "INSTALLMENT") {
                    database.debtDao().insertInstallmentDetails(
                        DebtInstallmentDetailsEntity(
                            debtId = debtId,
                            interestRate = doubleValOrNull("interestRate") ?: 0.0,
                            minimumPayment = doubleValOrNull("minimumPayment") ?: 0.0,
                            recommendedPayment = doubleValOrNull("recommendedPayment"),
                            paymentFrequency = stringVal("paymentFrequency").ifEmpty { "MONTHLY" },
                            priority = intVal("priority")
                        )
                    )
                }
            }
            "debt_payments" -> database.debtPaymentDao().insertPayment(
                DebtPaymentEntity(
                    id = longVal("id"),
                    debtId = longVal("debtId"),
                    accountId = longVal("accountId"),
                    amount = doubleVal("amount"),
                    paymentDate = longVal("paymentDate"),
                    paymentType = stringVal("paymentType"),
                    note = stringValOrNull("note"),
                    linkedTransactionId = longValOrNull("linkedTransactionId"),
                    createdAt = longVal("createdAt")
                )
            )
            "transfers" -> database.transferDao().insertTransfer(
                TransferEntity(
                    id = longVal("id"),
                    fromAccountId = longVal("fromAccountId"),
                    toAccountId = longVal("toAccountId"),
                    amount = doubleVal("amount"),
                    feeAmount = doubleValOrNull("feeAmount"),
                    note = stringValOrNull("note"),
                    date = longVal("date"),
                    referenceId = stringVal("referenceId"),
                    createdAt = longVal("createdAt")
                )
            )
            "budget_goals" -> database.budgetGoalDao().insertBudgetGoal(
                BudgetGoalEntity(
                    id = longVal("id"),
                    title = stringVal("title"),
                    linkedCategoryId = longValOrNull("linkedCategoryId"),
                    budgetType = stringVal("budgetType"),
                    amountLimit = doubleVal("amountLimit"),
                    spentAmount = doubleVal("spentAmount"),
                    startDate = longVal("startDate"),
                    endDate = longVal("endDate"),
                    alertThresholdPercent = intVal("alertThresholdPercent"),
                    isActive = boolVal("isActive"),
                    color = stringVal("color"),
                    icon = stringVal("icon"),
                    createdAt = longVal("createdAt")
                )
            )
            "financial_plans" -> database.financialPlanDao().insertPlan(
                FinancialPlanEntity(
                    id = longVal("id"),
                    title = stringVal("title"),
                    type = stringVal("type"),
                    targetAmount = doubleVal("targetAmount"),
                    currentAmount = doubleVal("currentAmount"),
                    linkedAccountIds = stringVal("linkedAccountIds"),
                    linkedCategoryIds = stringVal("linkedCategoryIds"),
                    startDate = longVal("startDate"),
                    endDate = longValOrNull("endDate"),
                    status = stringVal("status").ifEmpty { "ACTIVE" },
                    notes = stringValOrNull("notes"),
                    color = stringVal("color").ifEmpty { "#6C63FF" },
                    icon = stringVal("icon").ifEmpty { "flag" },
                    createdAt = longVal("createdAt")
                )
            )
            "transaction_templates" -> database.transactionTemplateDao().insertTemplate(
                TransactionTemplateEntity(
                    id = longVal("id"),
                    name = stringVal("name"),
                    amount = doubleVal("amount"),
                    transactionType = stringVal("transactionType"),
                    accountId = longVal("accountId"),
                    targetAccountId = longValOrNull("targetAccountId"),
                    categoryId = longValOrNull("categoryId"),
                    subcategoryId = longValOrNull("subcategoryId"),
                    notes = stringValOrNull("notes"),
                    iconEmoji = stringValOrNull("iconEmoji"),
                    colorHex = stringValOrNull("colorHex"),
                    isPinned = boolVal("isPinned"),
                    usageCount = intVal("usageCount"),
                    lastUsedAt = longValOrNull("lastUsedAt"),
                    createdAt = longVal("createdAt"),
                    updatedAt = longVal("updatedAt")
                )
            )
            "notifications" -> database.notificationDao().insertNotification(
                NotificationEntity(
                    id = longVal("id"),
                    title = stringVal("title"),
                    message = stringVal("message"),
                    type = stringVal("type"),
                    isRead = boolVal("isRead"),
                    timestamp = longVal("timestamp"),
                    deepLinkRoute = stringValOrNull("deepLinkRoute"),
                    relatedEntityId = longValOrNull("relatedEntityId")
                )
            )
            "category_rules" -> database.categoryRuleDao().insertRule(
                CategoryRuleEntity(
                    id = longVal("id"),
                    keyword = stringVal("keyword"),
                    categoryId = longVal("categoryId"),
                    priority = intVal("priority"),
                    source = stringVal("source").ifEmpty { "SYSTEM" },
                    isActive = boolVal("isActive")
                )
            )
            "user_category_mappings" -> database.userCategoryMappingDao().insertMapping(
                UserCategoryMappingEntity(
                    id = longVal("id"),
                    normalizedText = stringVal("normalizedText"),
                    categoryId = longVal("categoryId"),
                    usageCount = intVal("usageCount"),
                    lastUsedAt = longVal("lastUsedAt")
                )
            )
            "ai_chat_messages" -> database.aiChatDao().insertMessage(
                AiChatMessageEntity(
                    id = longVal("id"),
                    sender = stringVal("sender"),
                    message = stringVal("message"),
                    timestamp = longVal("timestamp"),
                    sessionTitle = stringVal("sessionTitle")
                )
            )
            "postal_profiles" -> database.postalProfileDao().insertProfile(
                PostalProfileEntity(
                    id = longVal("id"),
                    profileName = stringVal("profileName"),
                    firstName = stringVal("firstName"),
                    lastName = stringVal("lastName"),
                    fullName = stringVal("fullName"),
                    accountNumber = stringVal("accountNumber"),
                    accountKey = stringVal("accountKey"),
                    phone = stringValOrNull("phone"),
                    address = stringValOrNull("address"),
                    city = stringValOrNull("city"),
                    defaultRole = stringVal("defaultRole"),
                    isFavorite = boolVal("isFavorite"),
                    createdAt = longVal("createdAt"),
                    updatedAt = longVal("updatedAt")
                )
            )
            "salary_delays" -> database.salaryDelayDao().insertSalaryDelay(
                SalaryDelayEntity(
                    id = longVal("id"),
                    salaryId = longVal("salaryId"),
                    delayDays = intVal("delayDays"),
                    originalDate = longVal("originalDate"),
                    newDate = longVal("newDate"),
                    severityScore = intVal("severityScore"),
                    status = stringVal("status").ifEmpty { "CONFIRMED" },
                    createdAt = longVal("createdAt")
                )
            )
            "salary_distributions" -> database.salaryDistributionDao().insertDistribution(
                SalaryDistributionEntity(
                    id = longVal("id"),
                    salaryId = longVal("salaryId"),
                    isEnabled = boolVal("isEnabled"),
                    needsPercentage = intVal("needsPercentage"),
                    wantsPercentage = intVal("wantsPercentage"),
                    savingsPercentage = intVal("savingsPercentage"),
                    createdAt = longVal("createdAt"),
                    updatedAt = longVal("updatedAt")
                )
            )
            "salary_envelopes" -> database.salaryDistributionDao().insertEnvelope(
                SalaryEnvelopeEntity(
                    id = longVal("id"),
                    distributionId = longVal("distributionId"),
                    type = stringVal("type"),
                    label = stringVal("label"),
                    percentage = intVal("percentage"),
                    allocatedAmount = doubleVal("allocatedAmount"),
                    spentAmount = doubleVal("spentAmount"),
                    linkedCategoryIds = stringVal("linkedCategoryIds"),
                    linkedAccountId = longValOrNull("linkedAccountId"),
                    color = stringVal("color"),
                    icon = stringVal("icon")
                )
            )
        }
    }

    override fun isFolderUriValid(uriString: String?): Boolean {
        return backupManager?.isFolderUriValid(uriString) ?: false
    }

    override suspend fun exportBackupToFolder(
        folderUri: Uri,
        pwd: CharArray?,
        includeAttachments: Boolean,
        maxKeepBackups: Int,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)?
    ): Result<com.qdash.domain.model.BackupFileInfo> {
        return backupManager!!.exportBackupToFolder(folderUri, pwd, includeAttachments, maxKeepBackups, onProgress)
            .map { fileDetails ->
                com.qdash.domain.model.BackupFileInfo(
                    name = fileDetails.name,
                    sizeBytes = fileDetails.sizeBytes,
                    path = fileDetails.path
                )
            }
    }

    override suspend fun exportBackupV2(
        outputUri: Uri,
        pwd: CharArray?,
        includeAttachments: Boolean,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)?
    ): Result<Unit> {
        return backupManager!!.exportBackupV2(outputUri, pwd, includeAttachments, onProgress)
    }

    override suspend fun getRestorePreview(
        inputUri: Uri,
        pwd: CharArray?
    ): Result<com.qdash.domain.model.RestorePreview> {
        return backupManager!!.getRestorePreview(inputUri, pwd)
    }

    override suspend fun performRestoreV2(
        preview: com.qdash.domain.model.RestorePreview,
        selectedTables: List<String>?,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)?
    ): Result<Unit> {
        return backupManager!!.performRestoreV2(preview, selectedTables, onProgress)
    }

    override suspend fun exportBackup(uri: Uri): Result<Unit> {
        return backupManager!!.exportBackup(uri)
    }

    override suspend fun backupLocalJsonData(): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val json = exportAllDataAsJson()
            val encrypted = com.qdash.core.utils.CryptoUtils.encrypt(json.toString())
            val file = File(context!!.filesDir, "kdach_backup_drive.json")
            file.writeText(encrypted)
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val dateString = sdf.format(java.util.Date())
            preferencesManager!!.lastBackupDate = dateString
            Result.success(dateString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreLocalJsonData(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val file = File(context!!.filesDir, "kdach_backup_drive.json")
            if (!file.exists()) {
                return@withContext Result.failure(FileNotFoundException("ملف النسخة الاحتياطية غير موجود!"))
            }
            val encrypted = file.readText()
            val decrypted = try {
                com.qdash.core.utils.CryptoUtils.decrypt(encrypted)
            } catch (e: Exception) {
                encrypted
            }
            
            val json = JSONObject(decrypted)
            if (!json.has("accounts") || !json.has("transactions") || !json.has("categories")) {
                return@withContext Result.failure(IllegalArgumentException("الملف لا يحتوي على بيانات صالحة لتطبيق قداشّ."))
            }
            
            restoreFromJson(json)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasLocalJsonBackup(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        File(context!!.filesDir, "kdach_backup_drive.json").exists()
    }

    override suspend fun resetAllData(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            database.clearAllTables()
            preferencesManager!!.clearAll()
            DatabaseSeeder.prepopulateSystemDefaults(database)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun updateBackupSchedule(interval: String) {
        try {
            val workManager = androidx.work.WorkManager.getInstance(context!!)
            workManager.cancelUniqueWork("scheduled_backup")
            
            val repeatIntervalDays = when (interval) {
                "DAILY" -> 1L
                "WEEKLY" -> 7L
                "MONTHLY" -> 30L
                else -> 0L
            }
            
            if (repeatIntervalDays > 0L) {
                val backupWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.qdash.data.backup.ScheduledBackupWorker>(
                    repeatIntervalDays, java.util.concurrent.TimeUnit.DAYS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    "scheduled_backup",
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    backupWorkRequest
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
