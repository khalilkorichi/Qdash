package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val dbName = "kdach_database"
    private val backupFormatVersion = 1

    // Export local Room database + manifest + attachments into a single ZIP file
    suspend fun exportBackup(outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                    // 1. Generate and write manifest.json
                    val manifestJson = generateManifest()
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestJson.toByteArray())
                    zos.closeEntry()

                    // 2. Perform a Room database checkpoint and close database cleanly
                    database.close()

                    // 3. Write Room database files (db, shm, wal)
                    val dbFile = context.getDatabasePath(dbName)
                    val shmFile = File(dbFile.path + "-shm")
                    val walFile = File(dbFile.path + "-wal")

                    if (dbFile.exists()) {
                        zos.putNextEntry(ZipEntry("database/app.db"))
                        dbFile.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                    if (shmFile.exists()) {
                        zos.putNextEntry(ZipEntry("database/app.db-shm"))
                        shmFile.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                    if (walFile.exists()) {
                        zos.putNextEntry(ZipEntry("database/app.db-wal"))
                        walFile.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }

                    // 4. Write attachments if any exist
                    val attachmentsDir = File(context.filesDir, "attachments")
                    if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
                        attachmentsDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                zos.putNextEntry(ZipEntry("attachments/${file.name}"))
                                file.inputStream().use { input -> input.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Validate ZIP file contents and safely restore databases
    suspend fun importBackup(inputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        var tempDbFile: File? = null
        var tempShmFile: File? = null
        var tempWalFile: File? = null
        val resolver = context.contentResolver

        try {
            // 1. Fast validation check - Look for manifest.json and read it
            var manifestValid = false
            resolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "manifest.json") {
                            val manifestContent = zis.bufferedReader().readText()
                            manifestValid = validateManifest(manifestContent)
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            if (!manifestValid) {
                return@withContext Result.failure(Exception("ملف النسخ الاحتياطي غير صالح أو غير متوافق."))
            }

            // 2. Perform a safe backup of the current database before replacing it
            val dbFile = context.getDatabasePath(dbName)
            val shmFile = File(dbFile.path + "-shm")
            val walFile = File(dbFile.path + "-wal")

            if (dbFile.exists()) {
                tempDbFile = File(context.cacheDir, "temp_backup.db")
                dbFile.copyTo(tempDbFile, overwrite = true)
            }
            if (shmFile.exists()) {
                tempShmFile = File(context.cacheDir, "temp_backup.db-shm")
                shmFile.copyTo(tempShmFile, overwrite = true)
            }
            if (walFile.exists()) {
                tempWalFile = File(context.cacheDir, "temp_backup.db-wal")
                walFile.copyTo(tempWalFile, overwrite = true)
            }

            // 3. Close the active Room database instance to release file locks
            database.close()

            // 4. Overwrite databases from Zip entries
            resolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            "database/app.db" -> {
                                dbFile.outputStream().use { output -> zis.copyTo(output) }
                            }
                            "database/app.db-shm" -> {
                                shmFile.outputStream().use { output -> zis.copyTo(output) }
                            }
                            "database/app.db-wal" -> {
                                walFile.outputStream().use { output -> zis.copyTo(output) }
                            }
                            else -> {
                                if (entry.name.startsWith("attachments/")) {
                                    val fileName = entry.name.substringAfter("attachments/")
                                    val destFile = File(File(context.filesDir, "attachments"), fileName)
                                    destFile.parentFile?.mkdirs()
                                    destFile.outputStream().use { output -> zis.copyTo(output) }
                                }
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            // Clean up temporary backups on success
            tempDbFile?.delete()
            tempShmFile?.delete()
            tempWalFile?.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            // RESTORE database from safe backup files in case of failure!
            tempDbFile?.let {
                val dbFile = context.getDatabasePath(dbName)
                it.copyTo(dbFile, overwrite = true)
                it.delete()
            }
            tempShmFile?.let {
                val shmFile = File(context.getDatabasePath(dbName).path + "-shm")
                it.copyTo(shmFile, overwrite = true)
                it.delete()
            }
            tempWalFile?.let {
                val walFile = File(context.getDatabasePath(dbName).path + "-wal")
                it.copyTo(walFile, overwrite = true)
                it.delete()
            }
            Result.failure(e)
        }
    }

    private fun generateManifest(): String {
        val appVersionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        val json = JSONObject().apply {
            put("backupFormatVersion", backupFormatVersion)
            put("appVersion", appVersionName)
            put("dbVersion", database.openHelper.readableDatabase.version)
            put("createdAt", System.currentTimeMillis())
            put("deviceInfo", "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
        }
        return json.toString(4)
    }

    private fun validateManifest(content: String): Boolean {
        return try {
            val json = JSONObject(content)
            val formatVer = json.getInt("backupFormatVersion")
            val dbVer = json.getInt("dbVersion")
            // Backward compatibility checks
            formatVer == backupFormatVersion && dbVer <= database.openHelper.readableDatabase.version
        } catch (e: Exception) {
            false
        }
    }
}
