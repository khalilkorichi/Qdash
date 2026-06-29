package com.example.data

import com.example.data.local.dao.AiChatDao
import com.example.data.local.entities.AiChatMessageEntity
import com.example.data.repository.AiRepositoryImpl
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRepositoryTest {

    private val fakeAccountRepository = object : AccountRepository {
        override fun getAllAccounts(): Flow<List<Account>> = flowOf(
            listOf(
                Account(id = 1, name = "الجيبي", type = AccountType.CASH, balance = 5000.0, color = "#FF0000", icon = "💵"),
                Account(id = 2, name = "حساب CCP", type = AccountType.CCP, balance = 12000.0, color = "#00FF00", icon = "💳"),
                Account(id = 3, name = "بنك الخليج", type = AccountType.BANK, balance = 45000.0, color = "#0000FF", icon = "🏦")
            )
        )
        override fun getArchivedAccounts(): Flow<List<Account>> = throw UnsupportedOperationException()
        override suspend fun getAccountById(id: Long): Account? = null
        override suspend fun insertAccount(account: Account): Long = 0
        override suspend fun updateAccount(account: Account) {}
        override suspend fun deleteAccount(account: Account) {}
        override suspend fun archiveAccount(id: Long) {}
        override suspend fun unarchiveAccount(id: Long) {}
        override suspend fun setDefaultAccount(id: Long) {}
        override suspend fun getTransactionCountForAccount(id: Long): Int = 0
    }

    private val fakeCategoryRepository = object : CategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getRootCategories(): Flow<List<Category>> = throw UnsupportedOperationException()
        override fun getSubcategories(parentId: Long): Flow<List<Category>> = throw UnsupportedOperationException()
        override fun searchCategories(query: String): Flow<List<Category>> = throw UnsupportedOperationException()
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun getTransactionCountForCategory(id: Long): Int = 0
        override suspend fun insertCategory(category: Category): Long = 0
        override suspend fun insertCategories(categories: List<Category>) {}
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(category: Category) {}
        override suspend fun deleteSubcategoriesForParent(parentId: Long) {}
        override suspend fun mergeCategories(sourceCategoryId: Long, targetCategoryId: Long) {}
    }

    private val fakeTransactionRepository = object : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override fun searchTransactions(query: String): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun insertTransaction(transaction: Transaction): Long = 0
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun isTransactionAlreadyInserted(type: TransactionType, note: String, startDate: Long, endDate: Long): Boolean = false
        override fun getDailyFinancialAggregatesForRange(startDate: Long, endDate: Long): Flow<List<com.example.data.local.entities.DailyFinancialAggregateEntity>> = throw UnsupportedOperationException()
        override suspend fun deleteTransactionsBulk(ids: List<Long>) {}
        override suspend fun updateTransactionsCategoryBulk(ids: List<Long>, newCategoryId: Long) {}
        override suspend fun bulkEditTransactions(params: com.example.domain.usecase.transaction.BulkEditParams): Result<Int> = Result.success(0)
    }

    private val fakeBudgetGoalRepository = object : BudgetGoalRepository {
        override fun getAllBudgetGoals(): Flow<List<BudgetGoal>> = flowOf(emptyList())
        override fun getActiveBudgetGoals(): Flow<List<BudgetGoal>> = throw UnsupportedOperationException()
        override suspend fun getBudgetGoalById(id: Long): BudgetGoal? = null
        override suspend fun insertBudgetGoal(budgetGoal: BudgetGoal): Long = 0
        override suspend fun updateBudgetGoal(budgetGoal: BudgetGoal) {}
        override suspend fun deleteBudgetGoal(budgetGoal: BudgetGoal) {}
        override suspend fun updateSpentAmount(id: Long, spentAmount: Double) {}
    }

    private val fakeDebtRepository = object : DebtRepository {
        override fun getAllDebts(): Flow<List<Debt>> = throw UnsupportedOperationException()
        override fun getActiveDebts(): Flow<List<Debt>> = throw UnsupportedOperationException()
        override suspend fun getDebtById(id: Long): Debt? = null
        override suspend fun insertDebt(debt: Debt): Long = 0
        override suspend fun updateDebt(debt: Debt) {}
        override suspend fun deleteDebt(debt: Debt) {}
        override fun getAllPayments(): Flow<List<DebtPayment>> = throw UnsupportedOperationException()
        override fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPayment>> = throw UnsupportedOperationException()
        override suspend fun insertPayment(payment: DebtPayment): Long = 0
        override suspend fun deletePayment(payment: DebtPayment) {}
        override suspend fun deletePaymentsForDebt(debtId: Long) {}
    }

    private val fakeNotificationRepository = object : NotificationRepository {
        override fun getAllNotifications(): Flow<List<AppNotification>> = throw UnsupportedOperationException()
        override fun getUnreadNotifications(): Flow<List<AppNotification>> = throw UnsupportedOperationException()
        override fun getUnreadCount(): Flow<Int> = throw UnsupportedOperationException()
        override fun getNotificationsByType(type: NotificationType): Flow<List<AppNotification>> = throw UnsupportedOperationException()
        override suspend fun insertNotification(notification: AppNotification): Long = 0
        override suspend fun markAsRead(id: Long) {}
        override suspend fun markAllAsRead() {}
        override suspend fun deleteNotification(notification: AppNotification) {}
        override suspend fun clearAll() {}
    }

    private val fakeAiChatDao = object : AiChatDao {
        override fun getMessagesBySession(sessionTitle: String): Flow<List<AiChatMessageEntity>> = flowOf(emptyList())
        override fun getAllSessionTitles(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun insertMessage(message: AiChatMessageEntity): Long = 0
        override suspend fun clearHistory(sessionTitle: String) {}
        override suspend fun deleteSession(sessionTitle: String) {}
    }

    @Test
    fun testGeneralBalanceInquiry() = runBlocking {
        val aiRepository = AiRepositoryImpl(
            aiChatDao = fakeAiChatDao,
            transactionRepository = fakeTransactionRepository,
            accountRepository = fakeAccountRepository,
            categoryRepository = fakeCategoryRepository,
            budgetGoalRepository = fakeBudgetGoalRepository,
            debtRepository = fakeDebtRepository,
            notificationRepository = fakeNotificationRepository
        )

        // Query: "كم رصيدي؟" (General query)
        val response1 = aiRepository.generateResponse("كم رصيدي؟", "gemini-2.5-flash")
        assertTrue("Should contain total balance of 62000.0", response1.replyText.contains("62000.0"))
        assertTrue("Should suggest individual accounts check", response1.replyText.contains("تفاصيل كل حساب على حدة"))

        // Query: "كم رصيد المحفظة" (General query)
        val response2 = aiRepository.generateResponse("كم رصيد المحفظة", "gemini-2.5-flash")
        assertTrue("Should contain total balance of 62000.0", response2.replyText.contains("62000.0"))
        assertTrue("Should suggest individual accounts check", response2.replyText.contains("تفاصيل كل حساب على حدة"))
    }

    @Test
    fun testSpecificAccountBalanceInquiry() = runBlocking {
        val aiRepository = AiRepositoryImpl(
            aiChatDao = fakeAiChatDao,
            transactionRepository = fakeTransactionRepository,
            accountRepository = fakeAccountRepository,
            categoryRepository = fakeCategoryRepository,
            budgetGoalRepository = fakeBudgetGoalRepository,
            debtRepository = fakeDebtRepository,
            notificationRepository = fakeNotificationRepository
        )

        // Query: "رصيد CCP"
        val responseCCP = aiRepository.generateResponse("كم رصيد CCP؟", "gemini-2.5-flash")
        assertTrue("Should contain CCP balance 12000.0", responseCCP.replyText.contains("12000.0"))
        assertTrue("Should reference CCP", responseCCP.replyText.contains("CCP"))
        // It shouldn't contain the total balance if it matched a specific account
        assertTrue("Should not contain total balance", !responseCCP.replyText.contains("62000.0"))

        // Query: "كم رصيد البنك"
        val responseBank = aiRepository.generateResponse("كم رصيد البنك؟", "gemini-2.5-flash")
        assertTrue("Should contain bank balance 45000.0", responseBank.replyText.contains("45000.0"))
        assertTrue("Should reference bank", responseBank.replyText.contains("بنك"))
    }

    @Test
    fun testMockErrorTriggers() = runBlocking {
        val aiRepository = AiRepositoryImpl(
            aiChatDao = fakeAiChatDao,
            transactionRepository = fakeTransactionRepository,
            accountRepository = fakeAccountRepository,
            categoryRepository = fakeCategoryRepository,
            budgetGoalRepository = fakeBudgetGoalRepository,
            debtRepository = fakeDebtRepository,
            notificationRepository = fakeNotificationRepository
        )

        // Network error trigger
        var threwNetwork = false
        try {
            aiRepository.generateAiResponse("test_session", "network error", "gemini-2.5-flash")
        } catch (e: AiFailureException.NetworkFailure) {
            threwNetwork = true
            assertTrue("Network message should match mock error", e.message?.contains("الشبكة") == true)
        }
        assertTrue("Should throw NetworkFailure for network error query", threwNetwork)

        // AI service error trigger
        var threwAi = false
        try {
            aiRepository.generateAiResponse("test_session", "ai error", "gemini-2.5-flash")
        } catch (e: AiFailureException.AiServiceFailure) {
            threwAi = true
            assertTrue("AI service message should match mock error", e.message?.contains("الذكاء الاصطناعي") == true)
        }
        assertTrue("Should throw AiServiceFailure for ai error query", threwAi)
    }

    @Test
    fun testPreventDuplicateUserPromptOnRetry() = runBlocking {
        // Stateful DAO to track insertions
        val statefulDao = object : AiChatDao {
            val list = mutableListOf<AiChatMessageEntity>()
            override fun getMessagesBySession(sessionTitle: String): Flow<List<AiChatMessageEntity>> = 
                flowOf(list.filter { it.sessionTitle == sessionTitle })
            override fun getAllSessionTitles(): Flow<List<String>> = flowOf(list.map { it.sessionTitle }.distinct())
            override suspend fun insertMessage(message: AiChatMessageEntity): Long {
                list.add(message)
                return list.size.toLong()
            }
            override suspend fun clearHistory(sessionTitle: String) { list.clear() }
            override suspend fun deleteSession(sessionTitle: String) { list.clear() }
        }

        val aiRepository = AiRepositoryImpl(
            aiChatDao = statefulDao,
            transactionRepository = fakeTransactionRepository,
            accountRepository = fakeAccountRepository,
            categoryRepository = fakeCategoryRepository,
            budgetGoalRepository = fakeBudgetGoalRepository,
            debtRepository = fakeDebtRepository,
            notificationRepository = fakeNotificationRepository
        )

        val session = "test_retry_session"
        val prompt = "network error" // this fails and throws

        // First attempt: should insert prompt, then throw NetworkFailure
        try {
            aiRepository.generateAiResponse(session, prompt, "gemini-2.5-flash")
        } catch (e: AiFailureException.NetworkFailure) {
            // expected
        }

        // Verify it was inserted once
        assertEquals(1, statefulDao.list.size)
        assertEquals(prompt, statefulDao.list[0].message)
        assertEquals("USER", statefulDao.list[0].sender)

        // Second attempt (retry): should NOT insert it again, then throw NetworkFailure
        try {
            aiRepository.generateAiResponse(session, prompt, "gemini-2.5-flash")
        } catch (e: AiFailureException.NetworkFailure) {
            // expected
        }

        // Verify it is STILL inserted only once
        assertEquals(1, statefulDao.list.size)
    }
}

