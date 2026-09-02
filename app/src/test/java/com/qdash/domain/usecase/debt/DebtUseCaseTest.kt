package com.qdash.domain.usecase.debt

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.AccountEntity
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

    private lateinit var updateRegularDebtUseCase: UpdateRegularDebtUseCase
    private lateinit var updateInstallmentDebtUseCase: UpdateInstallmentDebtUseCase
    private lateinit var deleteDebtUseCase: DeleteDebtUseCase
    private lateinit var forgiveDebtUseCase: ForgiveDebtUseCase
    private lateinit var cancelDebtPaymentUseCase: CancelDebtPaymentUseCase
    private lateinit var addLentDebtUseCase: AddLentDebtUseCase
    private lateinit var recordLentDebtRepaymentUseCase: RecordLentDebtRepaymentUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Insert category entities to satisfy foreign key constraints
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
            db.categoryDao().insertCategory(
                com.qdash.data.local.entities.CategoryEntity(
                    id = 15L,
                    name = "Other Income",
                    type = "INCOME",
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

        updateRegularDebtUseCase = UpdateRegularDebtUseCase(debtRepo)
        updateInstallmentDebtUseCase = UpdateInstallmentDebtUseCase(debtRepo)
        deleteDebtUseCase = DeleteDebtUseCase(debtRepo, transactionRepo)
        forgiveDebtUseCase = ForgiveDebtUseCase(debtRepo)
        cancelDebtPaymentUseCase = CancelDebtPaymentUseCase(debtRepo, transactionRepo)
        addLentDebtUseCase = AddLentDebtUseCase(debtRepo, transactionRepo)
        recordLentDebtRepaymentUseCase = RecordLentDebtRepaymentUseCase(debtRepo, transactionRepo)
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
            InstallmentDebt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, interestRate = 0.0
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
        val currentDebt = debtRepo.getDebtById(debtId)!! as InstallmentDebt
        debtRepo.updateDebt(currentDebt.copy(remainingAmount = 8000.0))

        // 3. Test Update: New Total Amount is 9000 (which is > 2000 paid)
        val result = updateInstallmentDebtUseCase(
            debtId = debtId, title = "قرض معدل", creditorName = "أحمد", totalAmount = 9000.0,
            minimumPayment = 1000.0, paymentFrequency = "MONTHLY", linkedAccountId = accountId, priority = 3,
            notes = "تعديل", color = "#FFF", icon = "credit_card", interestRate = 0.0, dueDate = null
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
            InstallmentDebt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, interestRate = 0.0
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
        val currentDebt = debtRepo.getDebtById(debtId)!! as InstallmentDebt
        debtRepo.updateDebt(currentDebt.copy(remainingAmount = 6000.0))

        // 3. Test Update: New Total Amount is 3000 (which is < 4000 paid) -> should fail
        val result = updateInstallmentDebtUseCase(
            debtId = debtId, title = "قرض معدل", creditorName = "أحمد", totalAmount = 3000.0,
            minimumPayment = 1000.0, paymentFrequency = "MONTHLY", linkedAccountId = accountId, priority = 3,
            notes = "تعديل", color = "#FFF", icon = "credit_card", interestRate = 0.0, dueDate = null
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
            InstallmentDebt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, interestRate = 0.0
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
            InstallmentDebt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 10000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, interestRate = 0.0
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
            InstallmentDebt(
                title = "قرض", creditorName = "أحمد", totalAmount = 10000.0, remainingAmount = 8000.0,
                minimumPayment = 1000.0, paymentFrequency = "MONTHLY", priority = 3, color = "#FFF", icon = "",
                linkedAccountId = accountId, isClosed = true, interestRate = 0.0
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

    @Test
    fun testAddLentDebt_deductsWalletBalance_createsDebtWithOwedToMeDirectionAndInitialTransactionId() = runBlocking {
        // 1. Setup account with 10,000 DZD
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "محفظة السيولة", type = "CASH", balance = 10000.0, color = "#10B981", icon = "")
        )

        // 2. Lend 3,000 DZD to Karim
        val debtId = addLentDebtUseCase(
            title = "سلفة شراء حاسوب",
            debtorName = "كريم",
            totalAmount = 3000.0,
            linkedAccountId = accountId,
            dueDate = System.currentTimeMillis() + 86400000L * 30,
            notes = "اتفاق سداد خلال شهر",
            color = "#10B981"
        )

        // 3. Verify Account balance was deducted: 10,000 - 3,000 = 7,000
        val accAfterLend = db.accountDao().getAccountById(accountId)!!
        assertEquals(7000.0, accAfterLend.balance, 0.001)

        // 4. Verify Debt entity properties
        val debt = debtRepo.getDebtById(debtId)!!
        assertEquals("سلفة شراء حاسوب", debt.title)
        assertEquals("كريم", debt.creditorName)
        assertEquals(3000.0, debt.totalAmount, 0.001)
        assertEquals(3000.0, debt.remainingAmount, 0.001)
        assertEquals(DebtDirection.OWED_TO_ME, debt.direction)
        assertNotNull(debt.initialTransactionId)

        // 5. Verify transaction created in repository
        val tx = transactionRepo.getTransactionById(debt.initialTransactionId!!)!!
        assertEquals(3000.0, tx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(accountId, tx.accountId)
        assertEquals("اتفاق سداد خلال شهر [سلفة: سلفة شراء حاسوب]", tx.note)
    }

    @Test
    fun testRecordLentDebtRepayment_depositsToWallet_decreasesRemainingAmount_andClosesWhenZero() = runBlocking {
        // 1. Setup account and lend 4,000 DZD
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "الحساب الجاري", type = "BANK", balance = 10000.0, color = "#10B981", icon = "")
        )
        val debtId = addLentDebtUseCase(
            title = "سلفة سفر",
            debtorName = "سامي",
            totalAmount = 4000.0,
            linkedAccountId = accountId
        )
        // Balance is now 6000.0

        // 2. Receive first partial repayment of 1500 DZD
        val payment1Result = recordLentDebtRepaymentUseCase(
            debtId = debtId,
            receivingAccountId = accountId,
            amount = 1500.0,
            note = "الدفعة الأولى"
        )
        assertTrue(payment1Result > 0)

        // Verify wallet received 1500 DZD -> balance is 7500.0
        val accAfterPayment1 = db.accountDao().getAccountById(accountId)!!
        assertEquals(7500.0, accAfterPayment1.balance, 0.001)

        val debtAfterPayment1 = debtRepo.getDebtById(debtId)!!
        assertEquals(2500.0, debtAfterPayment1.remainingAmount, 0.001)
        assertFalse(debtAfterPayment1.isClosed)

        // 3. Receive final repayment of 2500 DZD
        val payment2Result = recordLentDebtRepaymentUseCase(
            debtId = debtId,
            receivingAccountId = accountId,
            amount = 2500.0,
            note = "الدفعة الأخيرة وإغلاق السلفة"
        )
        assertTrue(payment2Result > 0)

        // Wallet balance is back to 10,000.0
        val accFinal = db.accountDao().getAccountById(accountId)!!
        assertEquals(10000.0, accFinal.balance, 0.001)

        val debtFinal = debtRepo.getDebtById(debtId)!!
        assertEquals(0.0, debtFinal.remainingAmount, 0.001)
        assertTrue(debtFinal.isClosed)

        // Verify payments tracked
        val payments = debtRepo.getPaymentsForDebt(debtId).first()
        assertEquals(2, payments.size)
    }

    @Test
    fun testDeleteLentDebt_refundsInitialLentAmountToWallet_andRollsBackRepayments() = runBlocking {
        // 1. Setup account with 10,000 DZD
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "محفظة السيولة", type = "CASH", balance = 10000.0, color = "#10B981", icon = "")
        )

        // 2. Lend 5,000 DZD -> balance becomes 5000.0
        val debtId = addLentDebtUseCase(
            title = "سلفة شراء دراجة",
            debtorName = "عمر",
            totalAmount = 5000.0,
            linkedAccountId = accountId
        )
        assertEquals(5000.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)

        // 3. Receive repayment of 2,000 DZD -> balance becomes 7000.0
        recordLentDebtRepaymentUseCase(
            debtId = debtId,
            receivingAccountId = accountId,
            amount = 2000.0,
            note = "استرداد جزء"
        )
        assertEquals(7000.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)

        // 4. Delete the lent debt completely
        val delResult = deleteDebtUseCase(debtId)
        assertTrue(delResult.isSuccess)

        // 5. Verification:
        // - The repayment of 2000 was undone (-2000)
        // - The initial expense of 5000 was refunded (+5000)
        // Net wallet balance should return exactly to 10,000.0!
        val accAfterDelete = db.accountDao().getAccountById(accountId)!!
        assertEquals(10000.0, accAfterDelete.balance, 0.001)

        // Debt and payments removed
        assertNull(debtRepo.getDebtById(debtId))
        assertTrue(debtRepo.getPaymentsForDebt(debtId).first().isEmpty())
    }

    @Test
    fun testForgiveLentDebt_customizesForgivenessNoteForDebtor() = runBlocking {
        // 1. Setup account and lend 3,000 DZD
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "البنك", type = "REGULAR", balance = 5000.0, color = "#FFF", icon = "")
        )
        val debtId = addLentDebtUseCase(
            title = "سلفة مساعدة",
            debtorName = "بلال",
            totalAmount = 3000.0,
            linkedAccountId = accountId
        )

        // 2. Forgive the debtor
        val forgiveResult = forgiveDebtUseCase(debtId)
        assertTrue(forgiveResult.isSuccess)

        val forgivenDebt = debtRepo.getDebtById(debtId)!!
        assertEquals(0.0, forgivenDebt.remainingAmount, 0.001)
        assertTrue(forgivenDebt.isClosed)
        assertTrue(forgivenDebt.notes?.contains("تم الإعفاء من السلفة / مسامحة المدين") == true)
    }
}
