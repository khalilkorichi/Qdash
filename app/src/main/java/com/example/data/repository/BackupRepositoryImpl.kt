package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.domain.model.TransactionType
import com.example.domain.repository.BackupRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class BackupRepositoryImpl(
    private val database: AppDatabase
) : BackupRepository {

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
            if (!it.isSystem) {
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
            })
        }
        backupObj.put("transactions", txArray)

        // 4. Capture Income Sources
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

        // 5. Capture Saving Goals
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

        // 6. Capture Savings Contributions
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

        // 7. Capture Subscriptions
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

        // 8. Capture Debts
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

        // 9. Capture Debt Payments
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

        // 10. Capture Transfers
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

        // 11. Capture Budget Goals
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

        // 12. Capture Financial Plans
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

        // 13. Capture Transaction Templates
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

        // 14. Capture Notifications
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
        // Schema validation
        if (!json.has("accounts") || !json.has("transactions") || !json.has("categories")) {
            throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح أو لا يحتوي على الجداول الأساسية للتطبيق!")
        }

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

        // 2. Restore Categories (Only custom ones)
        val catsArray = json.optJSONArray("categories")
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

        // 5. Restore Saving Goals
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

        // 6. Restore Savings Contributions
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

        // 7. Restore Subscriptions
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

        // 8. Restore Debts
        val debtsArray = json.optJSONArray("debts")
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

        // 10. Restore Transfers
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

        // 11. Restore Budget Goals
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

        // 12. Restore Financial Plans
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

        // 13. Restore Transaction Templates
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

        // 14. Restore Notifications
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

        // 15. Regenerate daily financial aggregates
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
}
