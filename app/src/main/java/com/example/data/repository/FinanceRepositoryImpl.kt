package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entities.*
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.example.domain.usecase.transaction.BulkEditParams
import androidx.room.withTransaction
import com.example.domain.usecase.budget.GetBudgetAlertsUseCase
import com.example.domain.usecase.budget.GetBudgetGoalsUseCase
import com.example.data.local.AppDatabase
import com.example.core.utils.FormatterUtils

class TransactionRepositoryImpl(
    private val database: com.example.data.local.AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val budgetGoalRepositoryProvider: () -> BudgetGoalRepository,
    private val notificationRepositoryProvider: () -> NotificationRepository,
    private val getBudgetAlertsUseCaseProvider: () -> GetBudgetAlertsUseCase,
    private val getBudgetGoalsUseCaseProvider: () -> GetBudgetGoalsUseCase
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { list -> list.map { it.toDomain() } }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val resultId = database.withTransaction {
            if (transaction.type == TransactionType.TRANSFER) {
                if (transaction.transferId != null) {
                    // Mirrored transfer branch
                    val account = accountDao.getAccountById(transaction.accountId)
                        ?: throw IllegalArgumentException("الحساب المالي المحدد غير موجود!")
                    val offset = if (transaction.isDebit) -transaction.amount else transaction.amount
                    accountDao.updateAccount(account.copy(balance = account.balance + offset))
                    
                    val resultId = transactionDao.insertTransaction(transaction.toEntity())
                    syncAggregateForDate(transaction.date)
                    resultId
                } else {
                    // Initial transfer request: create two mirrored transactions with a shared transferId
                    val sharedId = "TRF-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                    val debitTx = transaction.copy(transferId = sharedId, isDebit = true, kind = TransactionKind.TRANSFER)
                    val creditTx = transaction.copy(
                        accountId = transaction.toAccountId ?: throw IllegalArgumentException("حساب الوجهة مطلوب للتحويل!"),
                        toAccountId = transaction.accountId,
                        transferId = sharedId,
                        isDebit = false,
                        kind = TransactionKind.TRANSFER
                    )
                    
                    val resultId = transactionDao.insertTransaction(debitTx.toEntity())
                    transactionDao.insertTransaction(creditTx.toEntity())
                    
                    val sourceAccount = accountDao.getAccountById(debitTx.accountId)
                    if (sourceAccount != null) {
                        accountDao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance - debitTx.amount))
                    }
                    val destAccount = accountDao.getAccountById(creditTx.accountId)
                    if (destAccount != null) {
                        accountDao.updateAccount(destAccount.copy(balance = destAccount.balance + creditTx.amount))
                    }
                    
                    syncAggregateForDate(transaction.date)
                    resultId
                }
            } else {
                // Normal transaction branch: INCOME, EXPENSE, SALARY, SAVINGS_CONTRIBUTION, SAVINGS_WITHDRAWAL
                val account = accountDao.getAccountById(transaction.accountId)
                    ?: throw IllegalArgumentException("الحساب المالي المحدد غير موجود في قاعدة البيانات!")

                val offset = when (transaction.kind) {
                    TransactionKind.EXPENSE, TransactionKind.SAVINGS_WITHDRAWAL -> -transaction.amount
                    TransactionKind.INCOME, TransactionKind.SALARY, TransactionKind.SAVINGS_CONTRIBUTION -> transaction.amount
                    TransactionKind.TRANSFER -> -transaction.amount
                }
                accountDao.updateAccount(account.copy(balance = account.balance + offset))

                val resultId = transactionDao.insertTransaction(transaction.toEntity())
                syncAggregateForDate(transaction.date)
                resultId
            }
        }

        checkAndTriggerNotifications(transaction, resultId)

        return resultId
    }

    private suspend fun checkAndTriggerNotifications(transaction: Transaction, transactionId: Long) {
        try {
            val notificationRepository = notificationRepositoryProvider()

            // 1. Check for Salary Added
            if (transaction.type == TransactionType.INCOME && transaction.categoryId != null) {
                val category = database.categoryDao().getCategoryById(transaction.categoryId)
                if (category != null && (category.name.contains("راتب") || category.name.contains("الراتب"))) {
                    val notification = AppNotification(
                        title = "تم إضافة الراتب 💰",
                        message = "تم إضافة راتب بقيمة ${transaction.amount.toInt()} د.ج إلى حسابك.",
                        type = NotificationType.SALARY_ADDED,
                        relatedEntityId = transactionId
                    )
                    notificationRepository.insertNotification(notification)
                }
            }

            // 2. Check for Budget Alerts (Exceeded or Critical)
            val isExpense = transaction.type == TransactionType.EXPENSE ||
                    transaction.kind == TransactionKind.EXPENSE ||
                    transaction.kind == TransactionKind.SAVINGS_WITHDRAWAL
            if (isExpense) {
                val budgetGoals = getBudgetGoalsUseCaseProvider()().first()
                val alerts = getBudgetAlertsUseCaseProvider()(budgetGoals)
                val existingNotifications = notificationRepository.getAllNotifications().first()
                val now = System.currentTimeMillis()

                for (alert in alerts) {
                    if (alert.status == BudgetStatus.EXCEEDED || alert.status == BudgetStatus.CRITICAL) {
                        // Throttling: Check if we already notified for this budget in the last 1 hour
                        val alreadyNotified = existingNotifications.any {
                            it.type == NotificationType.BUDGET_ALERT &&
                                    it.relatedEntityId == alert.budgetId &&
                                    (now - it.timestamp) < 60L * 60 * 1000 // 1 hour
                        }

                        if (!alreadyNotified) {
                            val title = if (alert.status == BudgetStatus.EXCEEDED) {
                                "تجاوز الميزانية! ⚠️"
                            } else {
                                "تنبيه ميزانية حرج! ⚠️"
                            }
                            val notification = AppNotification(
                                title = title,
                                message = alert.message,
                                type = NotificationType.BUDGET_ALERT,
                                relatedEntityId = alert.budgetId,
                                deepLinkRoute = "budget_goals"
                            )
                            notificationRepository.insertNotification(notification)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) = database.withTransaction {
        val oldTxEntity = transactionDao.getTransactionById(transaction.id) ?: return@withTransaction
        val oldTx = oldTxEntity.toDomain()
        val oldDate = oldTx.date
        
        // 1. Revert old balance effects
        if (oldTx.transferId != null) {
            val oldMirroredEntities = transactionDao.getTransactionsByTransferId(oldTx.transferId)
            oldMirroredEntities.forEach { entity ->
                val acc = accountDao.getAccountById(entity.accountId)
                if (acc != null) {
                    val revertOffset = if (entity.isDebit) entity.amount else -entity.amount
                    accountDao.updateAccount(acc.copy(balance = acc.balance + revertOffset))
                }
            }
        } else {
            val oldAccount = accountDao.getAccountById(oldTx.accountId)
            if (oldAccount != null) {
                val oldOffset = when (oldTx.kind) {
                    TransactionKind.EXPENSE, TransactionKind.SAVINGS_WITHDRAWAL -> -oldTx.amount
                    TransactionKind.INCOME, TransactionKind.SALARY, TransactionKind.SAVINGS_CONTRIBUTION -> oldTx.amount
                    TransactionKind.TRANSFER -> -oldTx.amount
                }
                accountDao.updateAccount(oldAccount.copy(balance = oldAccount.balance - oldOffset))
            }
            if (oldTx.type == TransactionType.TRANSFER && oldTx.toAccountId != null) {
                val oldDest = accountDao.getAccountById(oldTx.toAccountId)
                if (oldDest != null) {
                    accountDao.updateAccount(oldDest.copy(balance = oldDest.balance - oldTx.amount))
                }
            }
        }

        // 2. Apply new balance effects and update DB
        if (transaction.transferId != null) {
            val mirroredEntities = transactionDao.getTransactionsByTransferId(transaction.transferId)
            val debitEntity = mirroredEntities.find { it.isDebit }
            val creditEntity = mirroredEntities.find { it.isDebit.not() }

            if (debitEntity != null) {
                val updatedDebit = transaction.copy(
                    id = debitEntity.id,
                    isDebit = true,
                    accountId = transaction.accountId,
                    toAccountId = transaction.toAccountId
                )
                transactionDao.updateTransaction(updatedDebit.toEntity())
                val acc = accountDao.getAccountById(updatedDebit.accountId)
                if (acc != null) {
                    accountDao.updateAccount(acc.copy(balance = acc.balance - updatedDebit.amount))
                }
            }

            if (creditEntity != null) {
                val updatedCredit = transaction.copy(
                    id = creditEntity.id,
                    isDebit = false,
                    accountId = transaction.toAccountId ?: transaction.accountId,
                    toAccountId = transaction.accountId
                )
                transactionDao.updateTransaction(updatedCredit.toEntity())
                val acc = accountDao.getAccountById(updatedCredit.accountId)
                if (acc != null) {
                    accountDao.updateAccount(acc.copy(balance = acc.balance + updatedCredit.amount))
                }
            }
        } else {
            val newAccount = accountDao.getAccountById(transaction.accountId)
                ?: throw IllegalArgumentException("الحساب المالي الجديد المحدد غير موجود!")

            val offset = when (transaction.kind) {
                TransactionKind.EXPENSE, TransactionKind.SAVINGS_WITHDRAWAL -> -transaction.amount
                TransactionKind.INCOME, TransactionKind.SALARY, TransactionKind.SAVINGS_CONTRIBUTION -> transaction.amount
                TransactionKind.TRANSFER -> -transaction.amount
            }
            accountDao.updateAccount(newAccount.copy(balance = newAccount.balance + offset))
            
            transactionDao.updateTransaction(transaction.toEntity())
        }

        syncAggregateForDate(oldDate)
        syncAggregateForDate(transaction.date)
    }

    override suspend fun deleteTransaction(transaction: Transaction) = database.withTransaction {
        if (transaction.transferId != null) {
            val mirroredEntities = transactionDao.getTransactionsByTransferId(transaction.transferId)
            mirroredEntities.forEach { entity ->
                val acc = accountDao.getAccountById(entity.accountId)
                if (acc != null) {
                    val revertOffset = if (entity.isDebit) entity.amount else -entity.amount
                    accountDao.updateAccount(acc.copy(balance = acc.balance + revertOffset))
                }
            }
            transactionDao.deleteTransactionsByTransferId(transaction.transferId)
        } else {
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                val revertOffset = when (transaction.kind) {
                    TransactionKind.EXPENSE, TransactionKind.SAVINGS_WITHDRAWAL -> transaction.amount
                    TransactionKind.INCOME, TransactionKind.SALARY, TransactionKind.SAVINGS_CONTRIBUTION -> -transaction.amount
                    TransactionKind.TRANSFER -> transaction.amount
                }
                accountDao.updateAccount(account.copy(balance = account.balance + revertOffset))
            }
            if (transaction.type == TransactionType.TRANSFER && transaction.toAccountId != null) {
                val destAccount = accountDao.getAccountById(transaction.toAccountId)
                if (destAccount != null) {
                    accountDao.updateAccount(destAccount.copy(balance = destAccount.balance - transaction.amount))
                }
            }
            transactionDao.deleteTransaction(transaction.toEntity())
        }
        syncAggregateForDate(transaction.date)
    }

    override suspend fun deleteTransactionById(id: Long) = database.withTransaction {
        val tx = transactionDao.getTransactionById(id)
        if (tx != null) {
            deleteTransaction(tx.toDomain())
        }
    }

    override suspend fun isTransactionAlreadyInserted(type: TransactionType, note: String, startDate: Long, endDate: Long): Boolean {
        return transactionDao.isTransactionAlreadyInserted(type.name, note, startDate, endDate)
    }

    override fun getDailyFinancialAggregatesForRange(startDate: Long, endDate: Long): Flow<List<DailyFinancialAggregateEntity>> {
        return database.dailyFinancialAggregateDao().getAggregatesForRange(startDate, endDate)
    }

    override suspend fun deleteTransactionsBulk(ids: List<Long>) = database.withTransaction {
        val affectedDates = mutableSetOf<Long>()
        val processedTransferIds = mutableSetOf<String>()
        val idsToDelete = ids.toMutableSet()

        ids.forEach { id ->
            val tx = transactionDao.getTransactionById(id) ?: return@forEach
            affectedDates.add(tx.date)

            if (tx.transferId != null) {
                if (!processedTransferIds.contains(tx.transferId)) {
                    processedTransferIds.add(tx.transferId)
                    val mirrors = transactionDao.getTransactionsByTransferId(tx.transferId)
                    mirrors.forEach { entity ->
                        idsToDelete.add(entity.id)
                        val acc = accountDao.getAccountById(entity.accountId)
                        if (acc != null) {
                            val revertOffset = if (entity.isDebit) entity.amount else -entity.amount
                            accountDao.updateAccount(acc.copy(balance = acc.balance + revertOffset))
                        }
                    }
                }
            } else {
                val account = accountDao.getAccountById(tx.accountId)
                if (account != null) {
                    val kindEnum = try { TransactionKind.valueOf(tx.kind) } catch (e: Exception) { TransactionKind.INCOME }
                    val revertOffset = when (kindEnum) {
                        TransactionKind.EXPENSE, TransactionKind.SAVINGS_WITHDRAWAL -> tx.amount
                        TransactionKind.INCOME, TransactionKind.SALARY, TransactionKind.SAVINGS_CONTRIBUTION -> -tx.amount
                        TransactionKind.TRANSFER -> tx.amount
                    }
                    accountDao.updateAccount(account.copy(balance = account.balance + revertOffset))
                }
                if (tx.type == TransactionType.TRANSFER.name && tx.toAccountId != null) {
                    val destAccount = accountDao.getAccountById(tx.toAccountId)
                    if (destAccount != null) {
                        accountDao.updateAccount(destAccount.copy(balance = destAccount.balance - tx.amount))
                    }
                }
            }
        }

        transactionDao.deleteTransactionsBulk(idsToDelete.toList())
        affectedDates.forEach { syncAggregateForDate(it) }
    }

    override suspend fun updateTransactionsCategoryBulk(ids: List<Long>, newCategoryId: Long) = database.withTransaction {
        transactionDao.updateTransactionsCategoryBulk(ids, newCategoryId)
    }

    override suspend fun bulkEditTransactions(params: BulkEditParams): Result<Int> = database.withTransaction {
        try {
            val transactions = transactionDao.getTransactionsByIds(params.transactionIds)
            if (transactions.isEmpty()) return@withTransaction Result.success(0)

            // 1. Balance correction if account is changing
            if (params.newAccountId != null) {
                val destAccountId = params.newAccountId
                val groupedByAccount = transactions.groupBy { it.accountId }

                for ((originAccountId, txs) in groupedByAccount) {
                    if (originAccountId == destAccountId) continue

                    // Revert origin account balance
                    var originDelta = 0.0
                    for (tx in txs) {
                        val factor = when (tx.kind) {
                            "EXPENSE", "SAVINGS_WITHDRAWAL", "TRANSFER" -> tx.amount
                            "INCOME", "SALARY", "SAVINGS_CONTRIBUTION" -> -tx.amount
                            else -> tx.amount
                        }
                        originDelta += factor
                    }
                    accountDao.adjustBalance(originAccountId, originDelta)
                }

                // Deduct/apply to destination account balance
                var destDelta = 0.0
                for (tx in transactions) {
                    if (tx.accountId == destAccountId) continue
                    val factor = when (tx.kind) {
                        "EXPENSE", "SAVINGS_WITHDRAWAL", "TRANSFER" -> -tx.amount
                        "INCOME", "SALARY", "SAVINGS_CONTRIBUTION" -> tx.amount
                        else -> -tx.amount
                    }
                    destDelta += factor
                }
                accountDao.adjustBalance(destAccountId, destDelta)
            }

            // 2. Update entities with new category and/or account ID
            val updatedTransactions = transactions.map { tx ->
                tx.copy(
                    categoryId = params.newCategoryId ?: tx.categoryId,
                    accountId = params.newAccountId ?: tx.accountId
                )
            }
            transactionDao.updateTransactions(updatedTransactions)

            // 3. Sync aggregates
            val distinctDates = transactions.map { it.date }.distinct()
            distinctDates.forEach { syncAggregateForDate(it) }

            Result.success(updatedTransactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncAggregateForDate(date: Long) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 86400000L - 1L

        val txs = transactionDao.getTransactionsByDateRangeList(startOfDay, endOfDay)
        if (txs.isEmpty()) {
            database.dailyFinancialAggregateDao().deleteAggregateForDay(startOfDay)
        } else {
            val totalExp = txs.filter { 
                it.kind == TransactionKind.EXPENSE.name || 
                it.kind == TransactionKind.SAVINGS_WITHDRAWAL.name 
            }.sumOf { it.amount }
            val totalInc = txs.filter { 
                it.kind == TransactionKind.INCOME.name || 
                it.kind == TransactionKind.SALARY.name 
            }.sumOf { it.amount }
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

    override suspend fun getPeakTransactionHours(): List<Int> {
        try {
            val txs = getRecentTransactions(100).first()
            if (txs.size < 5) {
                return listOf(9, 14, 20)
            }

            val calendar = java.util.Calendar.getInstance()
            val hourGroups = txs.map {
                calendar.timeInMillis = it.date
                calendar.get(java.util.Calendar.HOUR_OF_DAY)
            }.groupBy { it }
            
            val sortedHours = hourGroups.entries
                .sortedByDescending { it.value.size }
                .map { it.key }
                .take(3)

            val result = sortedHours.toMutableList()
            val defaults = listOf(9, 14, 20)
            for (defaultHour in defaults) {
                if (result.size >= 3) break
                if (!result.contains(defaultHour)) {
                    result.add(defaultHour)
                }
            }
            return result.take(3)
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf(9, 14, 20)
        }
    }
}

class AccountRepositoryImpl(
    private val database: com.example.data.local.AppDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override fun getArchivedAccounts(): Flow<List<Account>> {
        return accountDao.getArchivedAccounts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun archiveAccount(id: Long) {
        accountDao.archiveAccount(id)
    }

    override suspend fun unarchiveAccount(id: Long) {
        accountDao.unarchiveAccount(id)
    }

    override suspend fun setDefaultAccount(id: Long) = database.withTransaction {
        accountDao.getAccountById(id) ?: throw IllegalArgumentException("الحساب المحدد غير موجود.")
        accountDao.clearDefaultFlag()
        val updatedRows = accountDao.setDefaultAccount(id)
        require(updatedRows == 1) { "تعذر تعيين الحساب الافتراضي." }
    }

    override suspend fun getTransactionCountForAccount(id: Long): Int {
        return transactionDao.getTransactionCountForAccount(id)
    }
}

class CategoryRepositoryImpl(
    private val database: com.example.data.local.AppDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetGoalDao: BudgetGoalDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getRootCategories(): Flow<List<Category>> {
        return categoryDao.getRootCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getSubcategories(parentId: Long): Flow<List<Category>> {
        return categoryDao.getSubcategories(parentId).map { list -> list.map { it.toDomain() } }
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        return categoryDao.searchCategories(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun getTransactionCountForCategory(id: Long): Int {
        return categoryDao.getTransactionCountForCategory(id)
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun deleteSubcategoriesForParent(parentId: Long) {
        categoryDao.deleteSubcategoriesForParent(parentId)
    }

    override suspend fun mergeCategories(sourceCategoryId: Long, targetCategoryId: Long) = database.withTransaction {
        require(sourceCategoryId != targetCategoryId) { "لا يمكن دمج الفئة مع نفسها." }
        val sourceCat = categoryDao.getCategoryById(sourceCategoryId)
            ?: throw IllegalArgumentException("الفئة المصدر غير موجودة.")
        categoryDao.getCategoryById(targetCategoryId)
            ?: throw IllegalArgumentException("الفئة الهدف غير موجودة.")
        transactionDao.mergeTransactionsCategory(sourceCategoryId, targetCategoryId)
        budgetGoalDao.mergeBudgetGoalsCategory(sourceCategoryId, targetCategoryId)
        categoryDao.deleteCategory(sourceCat)
    }
}

class IncomeRepositoryImpl(
    private val database: AppDatabase,
    private val incomeSourceDao: IncomeSourceDao,
    private val subscriptionDao: SubscriptionDao,
    private val salaryDelayDao: SalaryDelayDao,
    private val notificationDao: NotificationDao,
    private val context: android.content.Context
) : IncomeRepository {
    override fun getAllIncomeSources(): Flow<List<IncomeSource>> {
        return incomeSourceDao.getAllIncomeSources().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveIncomeSources(): Flow<List<IncomeSource>> {
        return incomeSourceDao.getActiveIncomeSources().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIncomeSourceById(id: Long): IncomeSource? {
        return incomeSourceDao.getIncomeSourceById(id)?.toDomain()
    }

    override suspend fun insertIncomeSource(incomeSource: IncomeSource): Long {
        return incomeSourceDao.insertIncomeSource(incomeSource.toEntity())
    }

    override suspend fun updateIncomeSource(incomeSource: IncomeSource) {
        incomeSourceDao.updateIncomeSource(incomeSource.toEntity())
    }

    override suspend fun deleteIncomeSource(incomeSource: IncomeSource) {
        incomeSourceDao.deleteIncomeSource(incomeSource.toEntity())
    }

    override fun getSalaryDelays(salaryId: Long): Flow<List<SalaryDelay>> {
        return salaryDelayDao.getSalaryDelays(salaryId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun confirmSalaryDelay(
        salaryId: Long,
        delayDays: Int,
        originalDate: Long,
        newDate: Long,
        severityScore: Int,
        affectedObligations: List<AffectedObligation>
    ) = database.withTransaction {
        // 1. Update salary nextExpectedDate
        val salaryEntity = incomeSourceDao.getIncomeSourceById(salaryId)
        if (salaryEntity != null) {
            incomeSourceDao.updateIncomeSource(salaryEntity.copy(nextExpectedDate = newDate))
        }

        // 2. Shift auto-shiftable obligations
        for (obs in affectedObligations) {
            if (obs.type == "SUBSCRIPTION" && obs.isAutoShiftable) {
                val subEntity = subscriptionDao.getSubscriptionById(obs.id)
                if (subEntity != null) {
                    val newBillingDate = subEntity.nextBillingDate + (delayDays * 86400000L)
                    subscriptionDao.updateSubscription(subEntity.copy(nextBillingDate = newBillingDate))
                }
            }
        }

        // 3. Insert SalaryDelay record
        val delayEntity = SalaryDelayEntity(
            salaryId = salaryId,
            delayDays = delayDays,
            originalDate = originalDate,
            newDate = newDate,
            severityScore = severityScore,
            status = "CONFIRMED",
            createdAt = System.currentTimeMillis()
        )
        salaryDelayDao.insertSalaryDelay(delayEntity)

        // 4. Create Notification Entity
        val formattedOriginalDate = FormatterUtils.formatShortDate(originalDate)
        val formattedNewDate = FormatterUtils.formatShortDate(newDate)
        
        val affectedText = if (affectedObligations.isNotEmpty()) {
            "مع تأثر ${affectedObligations.size} التزام مالي."
        } else {
            "بدون أي التزامات متأثرة."
        }

        val notification = NotificationEntity(
            title = "تأجيل تاريخ الراتب",
            message = "تم تأجيل موعد استلام الراتب من $formattedOriginalDate إلى $formattedNewDate بنجاح ($affectedText)",
            type = "SMART_REMINDER",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            deepLinkRoute = "salary",
            relatedEntityId = salaryId
        )
        val notificationId = notificationDao.insertNotification(notification)

        com.example.core.utils.SystemNotificationHelper.showNotification(
            context = context,
            appNotification = com.example.domain.model.AppNotification(
                id = notificationId,
                title = notification.title,
                message = notification.message,
                type = com.example.domain.model.NotificationType.SMART_REMINDER,
                isRead = false,
                timestamp = notification.timestamp,
                deepLinkRoute = notification.deepLinkRoute,
                relatedEntityId = notification.relatedEntityId
            )
        )
    }

    override suspend fun deleteSalaryDelay(delayId: Long) = database.withTransaction {
        val delayEntity = salaryDelayDao.getSalaryDelayById(delayId) ?: return@withTransaction

        // 1. Rollback nextExpectedDate of the salary
        val salaryEntity = incomeSourceDao.getIncomeSourceById(delayEntity.salaryId)
        if (salaryEntity != null) {
            incomeSourceDao.updateIncomeSource(salaryEntity.copy(nextExpectedDate = delayEntity.originalDate))
        }

        // 2. Rollback auto-shiftable obligations (subtract delayDays * 86400000L)
        val activeSubscriptions = subscriptionDao.getActiveSubscriptions().first()
        for (subEntity in activeSubscriptions) {
            if (subEntity.isAutoShiftableBySalary) {
                val newBillingDate = subEntity.nextBillingDate - (delayEntity.delayDays * 86400000L)
                subscriptionDao.updateSubscription(subEntity.copy(nextBillingDate = newBillingDate))
            }
        }

        // 3. Delete the delay record
        salaryDelayDao.deleteSalaryDelay(delayEntity)

        // 4. Create Notification Entity
        val formattedOriginalDate = FormatterUtils.formatShortDate(delayEntity.originalDate)
        val notification = NotificationEntity(
            title = "إلغاء تأجيل الراتب",
            message = "تم إلغاء تأجيل الراتب وإعادة جدولة الالتزامات إلى مواعيدها الأصلية قبل تاريخ $formattedOriginalDate.",
            type = "SMART_REMINDER",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            deepLinkRoute = "salary",
            relatedEntityId = delayEntity.salaryId
        )
        val notificationId = notificationDao.insertNotification(notification)

        com.example.core.utils.SystemNotificationHelper.showNotification(
            context = context,
            appNotification = com.example.domain.model.AppNotification(
                id = notificationId,
                title = notification.title,
                message = notification.message,
                type = com.example.domain.model.NotificationType.SMART_REMINDER,
                isRead = false,
                timestamp = notification.timestamp,
                deepLinkRoute = notification.deepLinkRoute,
                relatedEntityId = notification.relatedEntityId
            )
        )
    }

    override suspend fun updateSalaryDelay(
        delayId: Long,
        newDelayDays: Int,
        newDate: Long,
        newSeverityScore: Int,
        affectedObligations: List<AffectedObligation>
    ) = database.withTransaction {
        val delayEntity = salaryDelayDao.getSalaryDelayById(delayId) ?: return@withTransaction
        val oldDelayDays = delayEntity.delayDays

        // 1. Update salary nextExpectedDate
        val salaryEntity = incomeSourceDao.getIncomeSourceById(delayEntity.salaryId)
        if (salaryEntity != null) {
            incomeSourceDao.updateIncomeSource(salaryEntity.copy(nextExpectedDate = newDate))
        }

        // 2. Adjust auto-shiftable obligations nextBillingDate by the difference: newDelayDays - oldDelayDays
        val diffDays = newDelayDays - oldDelayDays
        val activeSubscriptions = subscriptionDao.getActiveSubscriptions().first()
        for (subEntity in activeSubscriptions) {
            if (subEntity.isAutoShiftableBySalary) {
                val newBillingDate = subEntity.nextBillingDate + (diffDays * 86400000L)
                subscriptionDao.updateSubscription(subEntity.copy(nextBillingDate = newBillingDate))
            }
        }

        // 3. Update the delay record
        val updatedDelay = delayEntity.copy(
            delayDays = newDelayDays,
            newDate = newDate,
            severityScore = newSeverityScore,
            createdAt = System.currentTimeMillis()
        )
        salaryDelayDao.updateSalaryDelay(updatedDelay)

        // 4. Create Notification Entity
        val formattedOriginalDate = FormatterUtils.formatShortDate(delayEntity.originalDate)
        val formattedNewDate = FormatterUtils.formatShortDate(newDate)
        val notification = NotificationEntity(
            title = "تعديل تأجيل الراتب",
            message = "تم تعديل تأجيل الراتب من $formattedOriginalDate إلى $formattedNewDate ($newDelayDays يوم).",
            type = "SMART_REMINDER",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            deepLinkRoute = "salary",
            relatedEntityId = delayEntity.salaryId
        )
        val notificationId = notificationDao.insertNotification(notification)

        com.example.core.utils.SystemNotificationHelper.showNotification(
            context = context,
            appNotification = com.example.domain.model.AppNotification(
                id = notificationId,
                title = notification.title,
                message = notification.message,
                type = com.example.domain.model.NotificationType.SMART_REMINDER,
                isRead = false,
                timestamp = notification.timestamp,
                deepLinkRoute = notification.deepLinkRoute,
                relatedEntityId = notification.relatedEntityId
            )
        )
    }
}

class SavingRepositoryImpl(
    private val savingGoalDao: SavingGoalDao,
    private val savingsContributionDao: SavingsContributionDao
) : SavingRepository {
    override fun getAllSavingGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getAllSavingGoals().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSavingGoalById(id: Long): SavingGoal? {
        return savingGoalDao.getSavingGoalById(id)?.toDomain()
    }

    override suspend fun insertSavingGoal(savingGoal: SavingGoal): Long {
        return savingGoalDao.insertSavingGoal(savingGoal.toEntity())
    }

    override suspend fun updateSavingGoal(savingGoal: SavingGoal) {
        savingGoalDao.updateSavingGoal(savingGoal.toEntity())
    }

    override suspend fun deleteSavingGoal(savingGoal: SavingGoal) {
        savingGoalDao.deleteSavingGoal(savingGoal.toEntity())
    }

    override fun getAllContributions(): Flow<List<SavingsContribution>> {
        return savingsContributionDao.getAllContributions().map { list -> list.map { it.toDomain() } }
    }

    override fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContribution>> {
        return savingsContributionDao.getContributionsForGoal(goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertContribution(contribution: SavingsContribution): Long {
        return savingsContributionDao.insertContribution(contribution.toEntity())
    }

    override suspend fun deleteContribution(contribution: SavingsContribution) {
        savingsContributionDao.deleteContribution(contribution.toEntity())
    }

    override suspend fun deleteContributionsForGoal(goalId: Long) {
        savingsContributionDao.deleteContributionsForGoal(goalId)
    }
}

class SubscriptionRepositoryImpl(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {
    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getAllSubscriptions().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getActiveSubscriptions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSubscriptionById(id: Long): Subscription? {
        return subscriptionDao.getSubscriptionById(id)?.toDomain()
    }

    override suspend fun insertSubscription(subscription: Subscription): Long {
        return subscriptionDao.insertSubscription(subscription.toEntity())
    }

    override suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription.toEntity())
    }

    override suspend fun deleteSubscription(subscription: Subscription) {
        subscriptionDao.deleteSubscription(subscription.toEntity())
    }
}

class SalaryDistributionRepositoryImpl(
    private val salaryDistributionDao: com.example.data.local.dao.SalaryDistributionDao
) : SalaryDistributionRepository {

    override fun getDistributionForSalary(salaryId: Long): Flow<SalaryDistribution?> {
        return salaryDistributionDao.getDistributionForSalary(salaryId).map { it?.toDomain() }
    }

    override suspend fun getDistributionForSalaryOnce(salaryId: Long): SalaryDistribution? {
        return salaryDistributionDao.getDistributionForSalaryOnce(salaryId)?.toDomain()
    }

    override fun getEnvelopesForDistribution(distributionId: Long): Flow<List<SalaryEnvelope>> {
        return salaryDistributionDao.getEnvelopesForDistribution(distributionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getEnvelopesForDistributionOnce(distributionId: Long): List<SalaryEnvelope> {
        return salaryDistributionDao.getEnvelopesForDistributionOnce(distributionId).map { it.toDomain() }
    }

    override suspend fun insertDistribution(distribution: SalaryDistribution): Long {
        return salaryDistributionDao.insertDistribution(distribution.toEntity())
    }

    override suspend fun updateDistribution(distribution: SalaryDistribution) {
        salaryDistributionDao.updateDistribution(distribution.toEntity())
    }

    override suspend fun saveEnvelopes(envelopes: List<SalaryEnvelope>) {
        if (envelopes.isEmpty()) return
        val distId = envelopes.first().distributionId
        salaryDistributionDao.deleteEnvelopesForDistribution(distId)
        salaryDistributionDao.insertEnvelopes(envelopes.map { it.toEntity() })
    }

    override suspend fun updateLinkedCategories(envelopeId: Long, categoryIds: List<Long>) {
        salaryDistributionDao.updateLinkedCategories(envelopeId, categoryIds.joinToString(","))
    }
}

