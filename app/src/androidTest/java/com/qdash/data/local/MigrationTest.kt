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

    @Test
    @Throws(IOException::class)
    fun migrate22To23() {
        // Create database with version 22 schema
        var db = helper.createDatabase(TEST_DB, 22)

        // Insert some dummy data to verify it survives migration
        db.execSQL("INSERT INTO accounts (id, name, type, balance, currency, color, icon, isDefault, isArchived, createdAt, sortOrder) " +
                "VALUES (1, 'Test Account', 'CASH', 1000.0, 'DZD', '#FFFFFF', 'wallet', 1, 0, 123456789, 0)")
        db.execSQL("INSERT INTO notifications (id, title, message, type, isRead, timestamp, deepLinkRoute, relatedEntityId) " +
                "VALUES (1, 'Title', 'Msg', 'TIP', 0, 123456789, NULL, NULL)")
        db.execSQL("INSERT INTO postal_profiles (id, profileName, firstName, lastName, fullName, accountNumber, accountKey, phone, address, city, defaultRole, isFavorite, createdAt, updatedAt) " +
                "VALUES (1, 'Profile', 'First', 'Last', 'Full', '123', '45', NULL, NULL, NULL, 'SELF', 0, 123456789, 123456789)")
        db.execSQL("INSERT INTO financial_plans (id, title, type, targetAmount, currentAmount, linkedAccountIds, linkedCategoryIds, startDate, endDate, status, notes, color, icon, createdAt) " +
                "VALUES (1, 'Plan', 'CUSTOM', 1000.0, 0.0, '', '', 123456789, NULL, 'ACTIVE', NULL, '#FFFFFF', 'flag', 123456789)")

        // Close it
        db.close()

        // Open and migrate to 23
        db = helper.runMigrationsAndValidate(TEST_DB, 23, true, *ALL_MIGRATIONS)

        // Verify data survived
        val cursor = db.query("SELECT * FROM accounts WHERE id = 1")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("name")) == "Test Account")
        cursor.close()

        // Verify indices exist by querying sqlite_master
        val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='accounts'")
        var hasIndex = false
        while (indexCursor.moveToNext()) {
            if (indexCursor.getString(0) == "index_accounts_isArchived_sortOrder_createdAt") {
                hasIndex = true
            }
        }
        indexCursor.close()
        assert(hasIndex) { "Missing index_accounts_isArchived_sortOrder_createdAt" }
    }

    @Test
    @Throws(IOException::class)
    fun migrate23To24() {
        var db = helper.createDatabase(TEST_DB, 23)
        db.execSQL("INSERT INTO user_profiles (id, name, email, birthDate, avatarUrl, isGoogleLinked) " +
                "VALUES (1, 'Test User', 'test@qdash.com', NULL, NULL, 0)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 24, true, *ALL_MIGRATIONS)

        val cursor = db.query("SELECT * FROM user_profiles WHERE id = 1")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("name")) == "Test User")
        cursor.close()

        val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='user_profiles'")
        var hasIndex = false
        while (indexCursor.moveToNext()) {
            if (indexCursor.getString(0) == "index_user_profiles_name") {
                hasIndex = true
            }
        }
        indexCursor.close()
        assert(hasIndex) { "Missing index_user_profiles_name" }
    }
}
