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
        override suspend fun getAccountById(id: Long) = if (id == 1L) dummyAccount else null
        override suspend fun insertAccount(account: Account) = 1L
        override suspend fun updateAccount(account: Account) {}
        override suspend fun deleteAccount(account: Account) {}
        override suspend fun archiveAccount(id: Long) {}
        override suspend fun unarchiveAccount(id: Long) {}
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
}
