package com.qdash.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qdash.core.data.ALL_MIGRATIONS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate21To22() {
        // Create earliest database with version 21 schema
        var db = helper.createDatabase(TEST_DB, 21)

        // Insert some dummy data to verify it survives migration
        db.execSQL("INSERT INTO accounts (id, name, type, balance, currency, color, icon, isDefault, isArchived, createdAt, sortOrder) " +
                "VALUES (1, 'Test Account', 'CASH', 1000.0, 'DZD', '#FFFFFF', 'wallet', 1, 0, 123456789, 0)")

        // Close it
        db.close()

        // Open and migrate to 22
        db = helper.runMigrationsAndValidate(TEST_DB, 22, true, *ALL_MIGRATIONS)

        // Verify data survived
        val cursor = db.query("SELECT * FROM accounts WHERE id = 1")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("name")) == "Test Account")
        cursor.close()

        // Verify new table exists and is empty
        val profileCursor = db.query("SELECT * FROM user_profiles")
        assert(!profileCursor.moveToFirst())
        profileCursor.close()
    }
}
