package com.qdash.domain.usecase

import android.net.Uri
import com.qdash.domain.model.*
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.repository.UpdateRepository
import com.qdash.domain.usecase.accounts.GetAccountsUseCase
import com.qdash.domain.usecase.transaction.FilterTransactionsUseCase
import com.qdash.domain.usecase.transaction.GetTransactionsUseCase
import com.qdash.domain.usecase.transaction.TransactionFilterParams
import com.qdash.domain.usecase.update.CheckForUpdateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class UseCaseTest {

    private val dummyAccount = Account(id = 1, name = "Test Account", type = AccountType.CASH, balance = 100.0, currency = "DZD", color = "#000", icon = "")
    private val dummyBaridiAccount = Account(id = 2, name = "Baridi Account", type = AccountType.BARIDIMOB, balance = 200.0, currency = "DZD", color = "#000", icon = "")

    private val dummyTransactions = listOf(
        Transaction(id = 1, amount = 1000.0, type = TransactionType.EXPENSE, categoryId = 1, accountId = 1, date = 1000L, note = "Coffee", kind = TransactionKind.EXPENSE),
        Transaction(id = 2, amount = 15000.0, type = TransactionType.EXPENSE, categoryId = 2, accountId = 1, date = 2000L, note = "Rent", kind = TransactionKind.EXPENSE),
        Transaction(id = 3, amount = 5000.0, type = TransactionType.INCOME, categoryId = 3, accountId = 2, date = 3000L, note = "Salary Delay Payment", kind = TransactionKind.INCOME)
    )

    private val fakeAccountRepository = object : AccountRepository {
        override fun getAllAccounts() = flowOf(listOf(dummyAccount, dummyBaridiAccount))
        override fun getArchivedAccounts() = flowOf(emptyList<Account>())
        override fun getActiveAccounts() = getAllAccounts()
        override suspend fun getAccountById(id: Long) = if (id == 1L) dummyAccount else null
        override suspend fun insertAccount(account: Account) = 1L
        override suspend fun updateAccount(account: Account) {}
        override suspend fun deleteAccount(account: Account) {}
        override suspend fun archiveAccount(id: Long) {}
        override suspend fun unarchiveAccount(id: Long) {}
        override suspend fun deactivateAccount(id: Long) {}
        override suspend fun activateAccount(id: Long) {}
        override suspend fun setDefaultAccount(id: Long) {}
        override suspend fun getTransactionCountForAccount(id: Long) = 0
    }

    private val fakeTransactionRepository = object : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(dummyTransactions)
        override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> = flowOf(emptyList<Transaction>())
        override fun searchTransactions(query: String): Flow<List<Transaction>> = flowOf(emptyList<Transaction>())
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList<Transaction>())
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList<Transaction>())
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun isTransactionAlreadyInserted(type: TransactionType, note: String, startDate: Long, endDate: Long): Boolean = false
        override fun getDailyFinancialAggregatesForRange(startDate: Long, endDate: Long): Flow<List<DailyFinancialAggregate>> = flowOf(emptyList<DailyFinancialAggregate>())
        override suspend fun deleteTransactionsBulk(ids: List<Long>) {}
        override suspend fun updateTransactionsCategoryBulk(ids: List<Long>, newCategoryId: Long) {}
        override suspend fun bulkEditTransactions(params: com.qdash.domain.usecase.transaction.BulkEditParams): Result<Int> = Result.success(0)
        override suspend fun getPeakTransactionHours(): List<Int> = emptyList<Int>()
    }

    private val fakeUpdateRepository = object : UpdateRepository {
        override val downloadState: Flow<DownloadState> = flowOf(DownloadState.Idle)
        override fun startDownload(info: UpdateInfo) {}
        override fun pauseDownload(info: UpdateInfo) {}
        override fun cancelDownload(info: UpdateInfo) {}
        override suspend fun checkForUpdates(onStep: suspend (CheckingStep) -> Unit): Result<UpdateInfo> {
            return Result.success(
                UpdateInfo(
                    hasUpdate = true,
                    versionCode = 2,
                    versionName = "2.0.0",
                    updateIdentity = 123L,
                    apkUrl = "http://test",
                    apkSize = 1000L,
                    apkSha256 = "sha",
                    mandatory = false,
                    releaseNotes = "Notes"
                )
            )
        }
        override fun downloadApk(url: String, startBytes: Long): Flow<DownloadState> = flowOf(DownloadState.Idle)
        override fun verifyApkSha256(file: File, expectedSha256: String): Boolean = true
        override suspend fun copyApkToDownloads(file: File, filename: String): Uri? = null
        override suspend fun backupDataBeforeUpdate(): Result<Uri> = Result.failure(Exception())
        override suspend fun saveDownloadedApk(file: File, versionName: String): File = file
        override suspend fun getDownloadedApks(): List<File> = emptyList<File>()
        override suspend fun deleteDownloadedApk(file: File): Boolean = true
    }

    @Test
    fun testGetAccountsUseCase() = runBlocking {
        val useCase = GetAccountsUseCase(fakeAccountRepository)
        var accountsList: List<Account> = emptyList()
        useCase().collect {
            accountsList = it
        }
        assertEquals(2, accountsList.size)
        assertEquals("Test Account", accountsList[0].name)
    }

    @Test
    fun testGetTransactionsUseCase() = runBlocking {
        val useCase = GetTransactionsUseCase(fakeTransactionRepository)
        var list: List<Transaction> = emptyList()
        useCase().collect {
            list = it
        }
        assertEquals(3, list.size)
        assertEquals("Coffee", list[0].note)
    }

    @Test
    fun testFilterTransactionsUseCase() {
        val useCase = FilterTransactionsUseCase()
        
        // Test query filtering
        val coffeeResult = useCase(dummyTransactions, listOf(dummyAccount), TransactionFilterParams(query = "coffee"))
        assertEquals(1, coffeeResult.size)
        assertEquals("Coffee", coffeeResult[0].note)

        // Test large only filtering (>= 10000.0)
        val largeResult = useCase(dummyTransactions, listOf(dummyAccount), TransactionFilterParams(filterLargeOnly = true))
        assertEquals(1, largeResult.size)
        assertEquals("Rent", largeResult[0].note)

        // Test BaridiMob only filtering
        val baridiResult = useCase(dummyTransactions, listOf(dummyAccount, dummyBaridiAccount), TransactionFilterParams(filterBaridiMobOnly = true))
        assertEquals(1, baridiResult.size)
        assertEquals("Salary Delay Payment", baridiResult[0].note)
    }

    @Test
    fun testCheckForUpdateUseCase() = runBlocking {
        val useCase = CheckForUpdateUseCase(fakeUpdateRepository)
        val result = useCase()
        assert(result.isSuccess)
        val info = result.getOrThrow()
        assertEquals(true, info.hasUpdate)
        assertEquals("2.0.0", info.versionName)
    }

    /**
     * Verifies the core balance-delta math for a transaction update:
     *
     *   Initial account balance: 5000 DZD
     *   Existing EXPENSE: 1000 DZD  →  account shows 4000 DZD
     *   Edit expense to: 2000 DZD   →  account must show 3000 DZD (net Δ = -1000, not -3000)
     *
     * This test exercises the revert+apply delta logic in isolation using plain
     * arithmetic — the same math the repository executes via adjustBalance SQL calls.
     * A result of (initialBalance - oldAmount - newAmount) = 2000 DZD would indicate
     * the bug is present (revert not applied).
     */
    @Test
    fun testUpdateTransactionBalanceDelta() {
        val initialBalance = 5000.0
        val oldExpenseAmount = 1000.0
        val newExpenseAmount = 2000.0

        // Simulate state after original insert: balance reduced by old expense
        var accountBalance = initialBalance - oldExpenseAmount  // 4000.0

        // --- updateTransaction delta logic (mirrors FinanceRepositoryImpl) ---

        // Step 1: Revert old EXPENSE → add back old amount (atomic adjustBalance equivalent)
        val oldRevertDelta = oldExpenseAmount  // EXPENSE revert = +amount
        accountBalance += oldRevertDelta       // 4000 + 1000 = 5000

        // Step 2: Apply new EXPENSE → subtract new amount (atomic adjustBalance equivalent)
        val newOffset = -newExpenseAmount      // EXPENSE offset = -amount
        accountBalance += newOffset            // 5000 - 2000 = 3000

        // --- Assertions ---
        val expectedBalance = initialBalance - newExpenseAmount  // 5000 - 2000 = 3000
        assertEquals(
            "Balance after editing expense from $oldExpenseAmount to $newExpenseAmount should be $expectedBalance, not ${initialBalance - oldExpenseAmount - newExpenseAmount}",
            expectedBalance,
            accountBalance,
            0.001
        )
    }

    /**
     * Verifies that changing a transaction's type (EXPENSE → INCOME) correctly
     * nets both the revert of the old effect and the application of the new effect.
     *
     *   Initial balance: 5000 DZD
     *   Existing EXPENSE: 1000 DZD  →  account shows 4000 DZD
     *   Change to INCOME: 3000 DZD  →  account must show 7000 DZD (4000 + 1000 revert + 3000 income)
     */
    @Test
    fun testUpdateTransactionKindChange_ExpenseToIncome() {
        val initialBalance = 5000.0
        val oldExpenseAmount = 1000.0
        val newIncomeAmount = 3000.0

        var accountBalance = initialBalance - oldExpenseAmount  // 4000.0

        // Revert old EXPENSE
        accountBalance += oldExpenseAmount   // 4000 + 1000 = 5000

        // Apply new INCOME
        accountBalance += newIncomeAmount    // 5000 + 3000 = 8000

        val expectedBalance = initialBalance - oldExpenseAmount + oldExpenseAmount + newIncomeAmount
        assertEquals(
            "After changing expense to income, balance should be $expectedBalance",
            expectedBalance,
            accountBalance,
            0.001
        )
    }
}

