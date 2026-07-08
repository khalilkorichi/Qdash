package com.qdash.domain.usecase.debt

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.AccountEntity
import com.qdash.data.local.entities.DebtEntity
import com.qdash.data.local.entities.DebtPaymentEntity
import com.qdash.data.repository.DebtRepositoryImpl
import com.qdash.data.repository.TransactionRepositoryImpl
import com.qdash.domain.model.*
import com.qdash.domain.repository.DebtRepository
import com.qdash.domain.repository.TransactionRepository
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
class DebtUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var debtRepo: DebtRepository

    private lateinit var updateDebtUseCase: UpdateDebtUseCase
    private lateinit var deleteDebtUseCase: DeleteDebtUseCase
    private lateinit var forgiveDebtUseCase: ForgiveDebtUseCase
    private lateinit var cancelDebtPaymentUseCase: CancelDebtPaymentUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Insert category entity to satisfy foreign key constraints
        runBlocking {
            db.categoryDao().insertCategory(
                com.qdash.data.local.entities.CategoryEntity(
                    id = DebtConstants.DEBT_EXPENSE_CATEGORY_ID,
                    name = "Debt Expense",
                    type = "EXPENSE",
                    icon = "",
                    color = ""
                )
            )
        }

        transactionRepo = TransactionRepositoryImpl(
            database = db,
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            budgetGoalRepositoryProvider = { throw UnsupportedOperationException() },
            notificationRepositoryProvider = { throw UnsupportedOperationException() },
            getBudgetAlertsUseCaseProvider = { throw UnsupportedOperationException() },
            getBudgetGoalsUseCaseProvider = { throw UnsupportedOperationException() }
        )

        debtRepo = DebtRepositoryImpl(
            debtDao = db.debtDao(),
            debtPaymentDao = db.debtPaymentDao()
        )

        updateDebtUseCase = UpdateDebtUseCase(debtRepo)
        deleteDebtUseCase = DeleteDebtUseCase(debtRepo, transactionRepo)
        forgiveDebtUseCase = ForgiveDebtUseCase(debtRepo)
        cancelDebtPaymentUseCase = CancelDebtPaymentUseCase(debtRepo, transactionRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUpdateDebt_success_whenNewTotalAmountIsEqualOrGreaterToPaid() = runBlocking {
        // 1. Insert Account & Debt
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "البنك", type = "REGULAR", balance = 5000.0, color = "#FFF", icon = "")
        )
        val debtId = debtRepo.insertDebt(
            Debt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, debtType = DebtType.INSTALLMENT
            )
        )

        // 2. Add a payment of 2000
        val txId = transactionRepo.insertTransaction(
            Transaction(
                amount = 2000.0, type = TransactionType.EXPENSE, categoryId = DebtConstants.DEBT_EXPENSE_CATEGORY_ID,
                accountId = accountId, note = "تسديد دين", date = System.currentTimeMillis()
            )
        )
        debtRepo.insertPayment(
            DebtPayment(debtId = debtId, accountId = accountId, amount = 2000.0, paymentDate = System.currentTimeMillis(), paymentType = DebtPaymentType.MANUAL, linkedTransactionId = txId)
        )

        // Update remaining manually as record usecase would, to keep repo mock accurate
        val currentDebt = debtRepo.getDebtById(debtId)!!
        debtRepo.updateDebt(currentDebt.copy(remainingAmount = 8000.0))

        // 3. Test Update: New Total Amount is 9000 (which is > 2000 paid)
        val result = updateDebtUseCase(
            debtId = debtId, title = "قرض معدل", creditorName = "أحمد", totalAmount = 9000.0,
            minimumPayment = 1000.0, paymentFrequency = "MONTHLY", linkedAccountId = accountId, priority = 3,
            notes = "تعديل", color = "#FFF", interestRate = 0.0, dueDate = null, debtType = DebtType.INSTALLMENT
        )

        assertTrue(result.isSuccess)
        val updatedDebt = debtRepo.getDebtById(debtId)!!
        assertEquals("قرض معدل", updatedDebt.title)
        assertEquals(9000.0, updatedDebt.totalAmount, 0.001)
        assertEquals(7000.0, updatedDebt.remainingAmount, 0.001) // 9000 total - 2000 paid = 7000 remaining
    }

    @Test
    fun testUpdateDebt_failure_whenNewTotalAmountIsLessThanPaid() = runBlocking {
        // 1. Insert Account & Debt
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "البنك", type = "REGULAR", balance = 5000.0, color = "#FFF", icon = "")
        )
        val debtId = debtRepo.insertDebt(
            Debt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, debtType = DebtType.INSTALLMENT
            )
        )

        // 2. Add payments totaling 4000
        val txId = transactionRepo.insertTransaction(
            Transaction(
                amount = 4000.0, type = TransactionType.EXPENSE, categoryId = DebtConstants.DEBT_EXPENSE_CATEGORY_ID,
                accountId = accountId, note = "تسديد دين", date = System.currentTimeMillis()
            )
        )
        debtRepo.insertPayment(
            DebtPayment(debtId = debtId, accountId = accountId, amount = 4000.0, paymentDate = System.currentTimeMillis(), paymentType = DebtPaymentType.MANUAL, linkedTransactionId = txId)
        )
        val currentDebt = debtRepo.getDebtById(debtId)!!
        debtRepo.updateDebt(currentDebt.copy(remainingAmount = 6000.0))

        // 3. Test Update: New Total Amount is 3000 (which is < 4000 paid) -> should fail
        val result = updateDebtUseCase(
            debtId = debtId, title = "قرض معدل", creditorName = "أحمد", totalAmount = 3000.0,
            minimumPayment = 1000.0, paymentFrequency = "MONTHLY", linkedAccountId = accountId, priority = 3,
            notes = "تعديل", color = "#FFF", interestRate = 0.0, dueDate = null, debtType = DebtType.INSTALLMENT
        )

        assertTrue(result.isFailure)
        assertEquals("المبلغ الإجمالي الجديد (3000.0 د.ج) لا يمكن أن يكون أقل من إجمالي المبالغ المدفوعة سابقاً (4000.0 د.ج)!", result.exceptionOrNull()?.message)
    }

    @Test
    fun testDeleteDebt_cascadingRollback() = runBlocking {
        // 1. Insert Account & Debt
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "البنك", type = "REGULAR", balance = 5000.0, color = "#FFF", icon = "")
        )
        val debtId = debtRepo.insertDebt(
            Debt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, debtType = DebtType.INSTALLMENT
            )
        )

        // 2. Add payment of 2000 (reverts balance to 3000)
        val txId = transactionRepo.insertTransaction(
            Transaction(
                amount = 2000.0, type = TransactionType.EXPENSE, categoryId = DebtConstants.DEBT_EXPENSE_CATEGORY_ID,
                accountId = accountId, note = "تسديد دين", date = System.currentTimeMillis()
            )
        )
        debtRepo.insertPayment(
            DebtPayment(debtId = debtId, accountId = accountId, amount = 2000.0, paymentDate = System.currentTimeMillis(), paymentType = DebtPaymentType.MANUAL, linkedTransactionId = txId)
        )

        // Verify account balance is 3000.0 before deleting debt
        val accBeforeDelete = db.accountDao().getAccountById(accountId)!!
        assertEquals(3000.0, accBeforeDelete.balance, 0.001)

        // 3. Delete debt
        val result = deleteDebtUseCase(debtId)
        assertTrue(result.isSuccess)

        // Verify debt is deleted
        assertNull(debtRepo.getDebtById(debtId))

        // Verify payments are deleted
        val payments = debtRepo.getPaymentsForDebt(debtId).first()
        assertTrue(payments.isEmpty())

        // Verify transaction is deleted
        val tx = transactionRepo.getTransactionById(txId)
        assertNull(tx)

        // Verify account balance is rolled back to 5000.0
        val accAfterDelete = db.accountDao().getAccountById(accountId)!!
        assertEquals(5000.0, accAfterDelete.balance, 0.001)
    }

    @Test
    fun testForgiveDebt_zerosRemainingAmountWithoutCreatingTransaction() = runBlocking {
        // 1. Insert Account & Debt
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "البنك", type = "REGULAR", balance = 5000.0, color = "#FFF", icon = "")
        )
        val debtId = debtRepo.insertDebt(
            Debt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, debtType = DebtType.INSTALLMENT
            )
        )

        // 2. Forgive debt
        val result = forgiveDebtUseCase(debtId)
        assertTrue(result.isSuccess)

        // Verify remaining amount is 0.0 and closed is true
        val forgivenDebt = debtRepo.getDebtById(debtId)!!
        assertEquals(0.0, forgivenDebt.remainingAmount, 0.001)
        assertTrue(forgivenDebt.isClosed)
        assertTrue(forgivenDebt.notes?.contains("تم الإعفاء من الدين") == true)

        // Verify account balance remains untouched (5000.0)
        val acc = db.accountDao().getAccountById(accountId)!!
        assertEquals(5000.0, acc.balance, 0.001)

        // Verify no new transaction is inserted
        val transactions = transactionRepo.getAllTransactions().first()
        assertTrue(transactions.isEmpty())
    }

    @Test
    fun testCancelDebtPayment_cascadingRollback() = runBlocking {
        // 1. Insert Account & Debt
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "البنك", type = "REGULAR", balance = 5000.0, color = "#FFF", icon = "")
        )
        val debtId = debtRepo.insertDebt(
            Debt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 8000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, debtType = DebtType.INSTALLMENT, isClosed = true
            )
        )

        // 2. Add payment of 2000 (reverts balance to 3000)
        val txId = transactionRepo.insertTransaction(
            Transaction(
                amount = 2000.0, type = TransactionType.EXPENSE, categoryId = DebtConstants.DEBT_EXPENSE_CATEGORY_ID,
                accountId = accountId, note = "تسديد دين", date = System.currentTimeMillis()
            )
        )
        val paymentId = debtRepo.insertPayment(
            DebtPayment(debtId = debtId, accountId = accountId, amount = 2000.0, paymentDate = System.currentTimeMillis(), paymentType = DebtPaymentType.MANUAL, linkedTransactionId = txId)
        )

        // 3. Cancel the payment
        val result = cancelDebtPaymentUseCase(paymentId)
        assertTrue(result.isSuccess)

        // Verify remaining amount is updated to 10000.0 and isClosed is false
        val updatedDebt = debtRepo.getDebtById(debtId)!!
        assertEquals(10000.0, updatedDebt.remainingAmount, 0.001)
        assertFalse(updatedDebt.isClosed)

        // Verify payment is deleted
        assertNull(debtRepo.getPaymentById(paymentId))

        // Verify transaction is deleted
        val tx = transactionRepo.getTransactionById(txId)
        assertNull(tx)

        // Verify account balance is rolled back to 5000.0
        val acc = db.accountDao().getAccountById(accountId)!!
        assertEquals(5000.0, acc.balance, 0.001)
    }
}
