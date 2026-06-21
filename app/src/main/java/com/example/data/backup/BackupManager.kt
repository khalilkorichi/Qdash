package com.example.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
                    database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
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

            // 2. Extract to staging first; live files are replaced only after validation.
            val dbFile = context.getDatabasePath(dbName)
            val shmFile = File(dbFile.path + "-shm")
            val walFile = File(dbFile.path + "-wal")
            val stagingDir = File(context.cacheDir, "backup_restore_staging").apply {
                deleteRecursively()
                mkdirs()
            }
            val stagedDbFile = File(stagingDir, "app.db")
            val stagedShmFile = File(stagingDir, "app.db-shm")
            val stagedWalFile = File(stagingDir, "app.db-wal")
            val stagedAttachmentsDir = File(stagingDir, "attachments").apply { mkdirs() }

            resolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            "database/app.db" -> stagedDbFile.outputStream().use { output -> zis.copyTo(output) }
                            "database/app.db-shm" -> stagedShmFile.outputStream().use { output -> zis.copyTo(output) }
                            "database/app.db-wal" -> stagedWalFile.outputStream().use { output -> zis.copyTo(output) }
                            else -> if (entry.name.startsWith("attachments/")) {
                                copyAttachmentEntrySafely(zis, entry, stagedAttachmentsDir)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            validateStagedDatabase(stagedDbFile)

            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

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

            // 4. Overwrite databases from validated staging files
            stagedDbFile.copyTo(dbFile, overwrite = true)
            if (stagedShmFile.exists()) stagedShmFile.copyTo(shmFile, overwrite = true) else shmFile.delete()
            if (stagedWalFile.exists()) stagedWalFile.copyTo(walFile, overwrite = true) else walFile.delete()

            val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
            stagedAttachmentsDir.listFiles()?.forEach { stagedAttachment ->
                if (stagedAttachment.isFile) {
                    stagedAttachment.copyTo(File(attachmentsDir, stagedAttachment.name), overwrite = true)
                }
            }

            // Clean up temporary backups on success
            tempDbFile?.delete()
            tempShmFile?.delete()
            tempWalFile?.delete()
            stagingDir.deleteRecursively()

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
            File(context.cacheDir, "backup_restore_staging").deleteRecursively()
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

    private fun copyAttachmentEntrySafely(zis: ZipInputStream, entry: ZipEntry, attachmentsDir: File) {
        if (entry.isDirectory) return
        val fileName = entry.name.substringAfter("attachments/")
        require(fileName.isNotBlank()) { "Invalid attachment entry." }
        require(!fileName.contains("..") && !File(fileName).isAbsolute) { "Invalid attachment path." }

        val canonicalDir = attachmentsDir.canonicalFile
        val destFile = File(canonicalDir, fileName).canonicalFile
        require(destFile.path.startsWith(canonicalDir.path + File.separator)) {
            "Attachment path escapes backup directory."
        }

        destFile.parentFile?.mkdirs()
        destFile.outputStream().use { output -> zis.copyTo(output) }
    }

    private fun validateStagedDatabase(stagedDbFile: File) {
        require(stagedDbFile.exists() && stagedDbFile.length() > 0L) {
            "ملف قاعدة البيانات غير موجود داخل النسخة الاحتياطية."
        }

        SQLiteDatabase.openDatabase(stagedDbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)) {
                    "قاعدة البيانات داخل النسخة الاحتياطية تالفة."
                }
            }
            require(db.version <= database.openHelper.readableDatabase.version) {
                "نسخة قاعدة البيانات في الملف أحدث من النسخة المدعومة."
            }
            listOf("accounts", "categories", "transactions").forEach { table ->
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
                    require(cursor.moveToFirst()) { "النسخة الاحتياطية تفتقد جدول $table." }
                }
            }
        }
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
