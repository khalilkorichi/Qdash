package com.qdash.core.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qdash.core.data.ALL_MIGRATIONS
import com.qdash.data.local.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test-full"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun testAllMigrationsFromV4ToV22() {
        // 1. Create database at version 4 schema
        var db = helper.createDatabase(TEST_DB, 4)

        // 2. Seed version 4 data (Account and Transaction)
        db.execSQL("""
            INSERT INTO accounts (id, name, type, balance, currency, color, icon, isDefault, isArchived, createdAt)
            VALUES (1, 'Cash Account', 'CASH', 1500.0, 'DZD', '#FF0000', 'wallet', 1, 0, 123456789)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO transactions (id, amount, type, categoryId, accountId, note, date, isRecurring)
            VALUES (10, 200.0, 'EXPENSE', 2, 1, 'Coffee', 123456790, 0)
        """.trimIndent())

        db.close()

        // 3. Migrate to version 5 to seed Debt
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, *ALL_MIGRATIONS)

        // Seed version 5 data (Debt)
        db.execSQL("""
            INSERT INTO debts (id, title, creditorName, totalAmount, remainingAmount, minimumPayment, paymentFrequency, priority, color, icon, createdAt, isClosed)
            VALUES (100, 'Car Loan', 'Bank', 50000.0, 45000.0, 1000.0, 'MONTHLY', 1, '#0000FF', 'car', 123456791, 0)
        """.trimIndent())

        db.close()

        // 4. Migrate the rest of the way to version 22
        db = helper.runMigrationsAndValidate(TEST_DB, 22, true, *ALL_MIGRATIONS)

        // 5. Assert all seeded data survived intact
        val accountCursor = db.query("SELECT * FROM accounts WHERE id = 1")
        assert(accountCursor.moveToFirst())
        assert(accountCursor.getString(accountCursor.getColumnIndex("name")) == "Cash Account")
        assert(accountCursor.getDouble(accountCursor.getColumnIndex("balance")) == 1500.0)
        accountCursor.close()

        val txCursor = db.query("SELECT * FROM transactions WHERE id = 10")
        assert(txCursor.moveToFirst())
        assert(txCursor.getString(txCursor.getColumnIndex("note")) == "Coffee")
        assert(txCursor.getDouble(txCursor.getColumnIndex("amount")) == 200.0)
        txCursor.close()

        val debtCursor = db.query("SELECT * FROM debts WHERE id = 100")
        assert(debtCursor.moveToFirst())
        assert(debtCursor.getString(debtCursor.getColumnIndex("title")) == "Car Loan")
        assert(debtCursor.getDouble(debtCursor.getColumnIndex("remainingAmount")) == 45000.0)
        debtCursor.close()

        // Verify the newly added user_profiles table from v22 exists
        val profileCursor = db.query("SELECT * FROM user_profiles")
        assert(!profileCursor.moveToFirst()) // Should be empty but must not throw table-not-found
        profileCursor.close()
    }
}
