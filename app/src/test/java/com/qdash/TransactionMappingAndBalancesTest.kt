package com.qdash

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.*
import com.qdash.data.repository.TransactionRepositoryImpl
import com.qdash.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionMappingAndBalancesTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepositoryImpl(
            database = db,
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            budgetGoalRepositoryProvider = { throw UnsupportedOperationException() },
            notificationRepositoryProvider = { throw UnsupportedOperationException() },
            getBudgetAlertsUseCaseProvider = { throw UnsupportedOperationException() },
            getBudgetGoalsUseCaseProvider = { throw UnsupportedOperationException() }
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testSavingsContribution_increasesSavingsBalance_excludesFromMonthlyIncome() = runBlocking {
        val accountId = db.accountDao().insertAccount(
            AccountEntity(
                name = "حساب التوفير",
                type = "SAVINGS",
                balance = 1000.0,
                color = "#4CAF50",
                icon = "savings"
            )
        )

        val tx = Transaction(
            amount = 500.0,
            type = TransactionType.INCOME,
            categoryId = 1L,
            accountId = accountId,
            note = "إيداع ادخار",
            date = System.currentTimeMillis(),
            kind = TransactionKind.SAVINGS_CONTRIBUTION
        )
        repository.insertTransaction(tx)

        val updatedAcc = db.accountDao().getAccountById(accountId)
        assertNotNull(updatedAcc)
        assertEquals(1500.0, updatedAcc!!.balance, 0.001)

        val aggregates = repository.getDailyFinancialAggregatesForRange(0L, Long.MAX_VALUE).first()
        if (aggregates.isNotEmpty()) {
            assertEquals(0.0, aggregates.first().totalIncome, 0.001)
        }
    }

    @Test
    fun testSavingsWithdrawal_decreasesSavingsBalance_excludesFromMonthlyIncome() = runBlocking {
        val accountId = db.accountDao().insertAccount(
            AccountEntity(
                name = "حساب التوفير",
                type = "SAVINGS",
                balance = 1000.0,
                color = "#4CAF50",
                icon = "savings"
            )
        )

        val tx = Transaction(
            amount = 300.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            accountId = accountId,
            note = "سحب ادخار",
            date = System.currentTimeMillis(),
            kind = TransactionKind.SAVINGS_WITHDRAWAL
        )
        repository.insertTransaction(tx)

        val updatedAcc = db.accountDao().getAccountById(accountId)
        assertNotNull(updatedAcc)
        assertEquals(700.0, updatedAcc!!.balance, 0.001)

        val aggregates = repository.getDailyFinancialAggregatesForRange(0L, Long.MAX_VALUE).first()
        if (aggregates.isNotEmpty()) {
            assertEquals(0.0, aggregates.first().totalIncome, 0.001)
            assertEquals(300.0, aggregates.first().totalExpense, 0.001)
        }
    }

    @Test
    fun testSalaryIncome_increasesBalance_includesInMonthlyIncome() = runBlocking {
        val accountId = db.accountDao().insertAccount(
            AccountEntity(
                name = "حساب البنك",
                type = "BANK",
                balance = 1000.0,
                color = "#2196F3",
                icon = "account_balance"
            )
        )

        val tx = Transaction(
            amount = 80000.0,
            type = TransactionType.INCOME,
            categoryId = 1L,
            accountId = accountId,
            note = "الراتب الشهري",
            date = System.currentTimeMillis(),
            kind = TransactionKind.SALARY
        )
        repository.insertTransaction(tx)

        val updatedAcc = db.accountDao().getAccountById(accountId)
        assertNotNull(updatedAcc)
        assertEquals(81000.0, updatedAcc!!.balance, 0.001)

        val aggregates = repository.getDailyFinancialAggregatesForRange(0L, Long.MAX_VALUE).first()
        assertTrue(aggregates.isNotEmpty())
        assertEquals(80000.0, aggregates.first().totalIncome, 0.001)
    }

    @Test
    fun testMirroredTransfer_createsDebitAndCredit_balanceUpdatedBothSides() = runBlocking {
        val sourceId = db.accountDao().insertAccount(
            AccountEntity(
                name = "الحساب الجاري",
                type = "CCP",
                balance = 5000.0,
                color = "#FF9800",
                icon = "wallet"
            )
        )
        val destId = db.accountDao().insertAccount(
            AccountEntity(
                name = "حساب التوفير",
                type = "SAVINGS",
                balance = 1000.0,
                color = "#4CAF50",
                icon = "savings"
            )
        )

        val transferTx = Transaction(
            amount = 2000.0,
            type = TransactionType.TRANSFER,
            categoryId = 12L,
            accountId = sourceId,
            toAccountId = destId,
            note = "تحويل للتوفير",
            date = System.currentTimeMillis()
        )
        repository.insertTransaction(transferTx)

        val updatedSource = db.accountDao().getAccountById(sourceId)
        assertNotNull(updatedSource)
        assertEquals(3000.0, updatedSource!!.balance, 0.001)

        val updatedDest = db.accountDao().getAccountById(destId)
        assertNotNull(updatedDest)
        assertEquals(3000.0, updatedDest!!.balance, 0.001)

        val transactions = db.transactionDao().getAllTransactions().first()
        val transfers = transactions.filter { it.type == "TRANSFER" }
        assertEquals(2, transfers.size)
        assertNotNull(transfers[0].transferId)
        assertEquals(transfers[0].transferId, transfers[1].transferId)
    }
}
