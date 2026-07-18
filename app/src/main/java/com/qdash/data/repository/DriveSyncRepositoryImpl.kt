package com.qdash.data.repository

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.qdash.core.preferences.PreferencesManager
import com.qdash.data.backup.BackupManager
import com.qdash.domain.model.BackupFileMetadata
import com.qdash.domain.repository.DriveSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class DriveSyncRepositoryImpl(
    private val backupManager: BackupManager,
    private val preferencesManager: PreferencesManager
) : DriveSyncRepository {

    private val _backupFoundToRestore = MutableStateFlow<BackupFileMetadata?>(null)
    override val backupFoundToRestore: StateFlow<BackupFileMetadata?> = _backupFoundToRestore.asStateFlow()

    override fun setBackupFoundToRestore(metadata: BackupFileMetadata?) {
        _backupFoundToRestore.value = metadata
    }

    override suspend fun uploadToAppData(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext Result.failure(Exception("حساب Google غير متصل."))

        val tempFile = File(context.cacheDir, "drive_sync_export.zip").apply {
            if (exists()) delete()
        }

        try {
            // 1. Export local database to temporary zip file
            val exportResult = backupManager.exportBackupV2(
                outputUri = Uri.fromFile(tempFile),
                password = null,
                includeAttachments = true
            )
            if (exportResult.isFailure) {
                return@withContext Result.failure(exportResult.exceptionOrNull() ?: Exception("فشل تصدير البيانات المحلية."))
            }

            // 2. Initialize Drive Client
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf("https://www.googleapis.com/auth/drive.appdata")
            ).setSelectedAccount(googleAccount.account)

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Qdash").build()

            // 3. Find if file already exists on Drive appDataFolder
            val query = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()
            val existingFile = query.files.find { it.name == "appdata.zip" }

            // 4. Upload/Update file
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "appdata.zip"
                if (existingFile == null) {
                    parents = listOf("appDataFolder")
                }
            }
            val mediaContent = FileContent("application/zip", tempFile)
            if (existingFile != null) {
                driveService.files().update(existingFile.id, fileMetadata, mediaContent).execute()
            } else {
                driveService.files().create(fileMetadata, mediaContent).execute()
            }

            // 5. Update last sync time
            preferencesManager.lastSyncTimestamp = System.currentTimeMillis()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    override suspend fun downloadFromAppData(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext Result.failure(Exception("حساب Google غير متصل."))

        val tempFile = File(context.cacheDir, "drive_sync_import.zip").apply {
            if (exists()) delete()
        }

        try {
            // 1. Initialize Drive Client
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf("https://www.googleapis.com/auth/drive.appdata")
            ).setSelectedAccount(googleAccount.account)

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Qdash").build()

            // 2. Find if backup file exists
            val query = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()
            val existingFile = query.files.find { it.name == "appdata.zip" }
                ?: return@withContext Result.success(false) // No file on Drive, meaning new account

            // 3. Download Drive file content
            tempFile.outputStream().use { fos ->
                driveService.files().get(existingFile.id).executeMediaAndDownloadTo(fos)
            }

            // 4. Perform Restore using BackupManager (Staging + Rollback verification)
            val previewResult = backupManager.getRestorePreview(
                inputUri = Uri.fromFile(tempFile),
                password = null
            )
            if (previewResult.isFailure) {
                return@withContext Result.failure(previewResult.exceptionOrNull() ?: Exception("فشل قراءة ملف النسخة الاحتياطية."))
            }

            val preview = previewResult.getOrThrow()
            val restoreResult = backupManager.performRestoreV2(
                preview = preview,
                selectedTables = null // Restore all tables
            )
            if (restoreResult.isFailure) {
                return@withContext Result.failure(restoreResult.exceptionOrNull() ?: Exception("فشل استعادة البيانات."))
            }

            // 5. Update last sync time
            preferencesManager.lastSyncTimestamp = System.currentTimeMillis()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    override suspend fun checkIfBackupExists(context: Context): Result<BackupFileMetadata?> = withContext(Dispatchers.IO) {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext Result.failure(Exception("حساب Google غير متصل."))

        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf("https://www.googleapis.com/auth/drive.appdata")
            ).setSelectedAccount(googleAccount.account)

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Qdash").build()

            val query = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, modifiedTime)")
                .execute()
            val existingFile = query.files.find { it.name == "appdata.zip" }

            if (existingFile != null) {
                val modifiedTime = existingFile.modifiedTime?.value ?: System.currentTimeMillis()
                Result.success(BackupFileMetadata(existingFile.id, existingFile.name, modifiedTime))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
