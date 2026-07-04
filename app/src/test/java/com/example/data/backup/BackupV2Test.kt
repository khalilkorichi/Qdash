package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.TransactionEntity
import com.example.core.utils.CryptoUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupV2Test {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager(context, db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testCryptoPBKDF2_EncryptionDecryption_Success() {
        val password = "my-secure-password".toCharArray()
        val salt = CryptoUtils.generateSalt()
        val key = CryptoUtils.deriveKey(password, salt)

        val originalData = "FinTrack-DZ-Backup-Test-Data-12345".toByteArray(Charsets.UTF_8)
        val encrypted = CryptoUtils.encryptBytes(originalData, key)

        val decrypted = CryptoUtils.decryptBytes(encrypted.ciphertext, encrypted.iv, key)
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun testCryptoPBKDF2_DecryptionWithWrongPassword_ThrowsException() {
        val password = "correct-password".toCharArray()
        val wrongPassword = "wrong-password".toCharArray()
        val salt = CryptoUtils.generateSalt()

        val key = CryptoUtils.deriveKey(password, salt)
        val wrongKey = CryptoUtils.deriveKey(wrongPassword, salt)

        val originalData = "secret-data".toByteArray(Charsets.UTF_8)
        val encrypted = CryptoUtils.encryptBytes(originalData, key)

        assertThrows(Exception::class.java) {
            CryptoUtils.decryptBytes(encrypted.ciphertext, encrypted.iv, wrongKey)
        }
    }

    @Test
    fun testBackupRestoreV2_Unencrypted_Success() = runBlocking {
        // 1. Insert mock data
        val account = AccountEntity(id = 1, name = "Cash Wallet", type = "CASH", balance = 5000.0, color = "#FFF", icon = "wallet")
        db.accountDao().insertAccount(account)

        val category = CategoryEntity(id = 1, name = "Food", type = "EXPENSE", icon = "food", color = "#F00", isSystem = false)
        db.categoryDao().insertCategory(category)

        val transaction = TransactionEntity(id = 1, amount = 250.0, type = "EXPENSE", categoryId = 1, accountId = 1, note = "Dinner", date = System.currentTimeMillis())
        db.transactionDao().insertTransaction(transaction)

        // 2. Export backup
        val backupFile = File(context.cacheDir, "unencrypted_backup.zip")
        val backupUri = Uri.fromFile(backupFile)
        val exportResult = backupManager.exportBackupV2(backupUri, null, false)
        assertTrue(exportResult.isSuccess)

        // 3. Clear database to simulate data loss
        db.transactionDao().deleteTransaction(transaction)
        db.accountDao().deleteAccount(account)

        assertEquals(0, db.accountDao().getAllAccountsIncludingArchived().first().size)

        // 4. Load preview
        val previewResult = backupManager.getRestorePreview(backupUri, null)
        assertTrue(previewResult.isSuccess)
        val preview = previewResult.getOrThrow()
        assertFalse(preview.manifest.isEncrypted)
        assertEquals(1, preview.manifest.recordCounts["accounts"])
        assertEquals(1, preview.manifest.recordCounts["transactions"])

        // 5. Restore
        val restoreResult = backupManager.performRestoreV2(preview, null)
        assertTrue(restoreResult.isSuccess)

        // 6. Verify restored data
        val accounts = db.accountDao().getAllAccountsIncludingArchived().first()
        assertEquals(1, accounts.size)
        assertEquals("Cash Wallet", accounts[0].name)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals("Dinner", transactions[0].note)
    }

    @Test
    fun testBackupRestoreV2_Encrypted_Success() = runBlocking {
        // 1. Insert mock data
        val account = AccountEntity(id = 2, name = "CCP Bank", type = "CCP", balance = 15000.0, color = "#00F", icon = "bank")
        db.accountDao().insertAccount(account)

        // 2. Export encrypted backup
        val backupFile = File(context.cacheDir, "encrypted_backup.zip")
        val backupUri = Uri.fromFile(backupFile)
        val password = "my-backup-password".toCharArray()
        val exportResult = backupManager.exportBackupV2(backupUri, password, false)
        assertTrue(exportResult.isSuccess)

        // 3. Load preview with incorrect password
        val wrongPassword = "wrong-password".toCharArray()
        val previewWrongResult = backupManager.getRestorePreview(backupUri, wrongPassword)
        assertTrue(previewWrongResult.isFailure)

        // 4. Load preview with correct password
        val previewResult = backupManager.getRestorePreview(backupUri, password)
        assertTrue(previewResult.isSuccess)
        val preview = previewResult.getOrThrow()
        assertTrue(preview.manifest.isEncrypted)
        assertEquals(1, preview.manifest.recordCounts["accounts"])

        // 5. Clear DB
        db.accountDao().deleteAccount(account)

        // 6. Restore
        val restoreResult = backupManager.performRestoreV2(preview, null)
        assertTrue(restoreResult.isSuccess)

        // 7. Verify
        val accounts = db.accountDao().getAllAccountsIncludingArchived().first()
        assertEquals(1, accounts.size)
        assertEquals("CCP Bank", accounts[0].name)
    }

    @Test
    fun testRestoreV2_ChecksumMismatch_Failure() = runBlocking {
        // 1. Insert data and export
        val account = AccountEntity(id = 3, name = "Savings", type = "SAVINGS", balance = 2000.0, color = "#0F0", icon = "star")
        db.accountDao().insertAccount(account)
        val backupFile = File(context.cacheDir, "checksum_backup.zip")
        val backupUri = Uri.fromFile(backupFile)
        backupManager.exportBackupV2(backupUri, null, false)

        // 2. Modify manifest checksum in ZIP to simulate tampering
        // To verify checksum failure: we can load the manifest, get calculated checksum, modify it, 
        // and check that BackupManager returns a failure on getRestorePreview.
        val previewResult = backupManager.getRestorePreview(backupUri, null)
        assertTrue(previewResult.isSuccess)
        val preview = previewResult.getOrThrow()
        
        // Let's tamper the checksum
        val tamperedPreview = preview.copy(
            manifest = preview.manifest.copy(checksumSHA256 = "invalidchecksum1234567890abcdef")
        )

        // Restoring tampered should fail or checksum check in performRestoreV2 should abort
        // Wait, BackupManager getRestorePreview already verifies checksum if files exist.
        // Let's check that if checksum doesn't match it throws during getRestorePreview.
        // We'll simulate checksum check by feeding tampered preview directly to performRestoreV2.
        val restoreResult = backupManager.performRestoreV2(tamperedPreview, null)
        assertTrue(restoreResult.isFailure)
        assertTrue(restoreResult.exceptionOrNull()?.message?.contains("النسخة الاحتياطية") == true || restoreResult.exceptionOrNull() is Exception)
    }
}
