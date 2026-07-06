package com.qdash.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.qdash.data.local.AppDatabase
import com.qdash.data.repository.BackupRepositoryImpl
import com.qdash.core.utils.CryptoUtils
import com.qdash.domain.model.BackupManifestV2
import com.qdash.domain.model.RestorePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val dbName = "kdach_database"
    private val legacyBackupFormatVersion = 1
    private val currentSchemaVersion = 2 // Unified JSON_V2 schema version

    private val backupRepository = BackupRepositoryImpl(database = database)

    data class FileDetails(val name: String, val sizeBytes: Long, val path: String)

    // --- New V2 Unified Backup API ---

    suspend fun exportBackupV2(
        outputUri: Uri,
        password: CharArray?,
        includeAttachments: Boolean,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "backup_export_temp").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            onProgress?.invoke("جاري فحص وتجهيز قاعدة البيانات...", 15)
            val rawJsonFile = File(tempDir, "data.json")
            val gzipJsonFile = File(tempDir, "data.json.gz")

            onProgress?.invoke("تجميع وتحويل جداول قاعدة البيانات إلى صيغة JSON...", 30)
            // 1. Serialize DB to JSON stream
            FileOutputStream(rawJsonFile).use { fos ->
                backupRepository.exportBackupV2(fos)
            }

            onProgress?.invoke("جاري ضغط البيانات باستخدام GZIP...", 65)
            // 2. Compress JSON to GZIP
            gzipCompress(rawJsonFile, gzipJsonFile)

            // 3. Encrypt GZIP if password provided
            val (payloadFile, salt, iv, isEncrypted) = if (password != null && password.isNotEmpty()) {
                onProgress?.invoke("تشفير ملف البيانات لحماية الخصوصية...", 80)
                val saltBytes = CryptoUtils.generateSalt()
                val key = CryptoUtils.deriveKey(password, saltBytes)
                val plainBytes = gzipJsonFile.readBytes()
                val encrypted = CryptoUtils.encryptBytes(plainBytes, key)

                val encFile = File(tempDir, "data.json.gz.enc")
                encFile.writeBytes(android.util.Base64.decode(encrypted.ciphertext, android.util.Base64.NO_WRAP))

                val ivBytes = android.util.Base64.decode(encrypted.iv, android.util.Base64.NO_WRAP)
                // We'll write the IV to the file itself or keep it.
                val combined = ByteArray(12 + encFile.length().toInt())
                System.arraycopy(ivBytes, 0, combined, 0, 12)
                System.arraycopy(encFile.readBytes(), 0, combined, 12, encFile.length().toInt())
                encFile.writeBytes(combined)

                Quadruple(encFile, saltBytes, ivBytes, true)
            } else {
                Quadruple(gzipJsonFile, null, null, false)
            }

            onProgress?.invoke("حساب البصمة الرقمية للتحقق من سلامة البيانات...", 90)
            // 4. Generate Checksum
            val checksum = calculateSHA256(payloadFile)

            // 5. Generate Manifest
            val recordCounts = getRecordCounts()
            val manifestJson = generateManifestV2(isEncrypted, salt, checksum, recordCounts)

            onProgress?.invoke("إنشاء حزمة النسخ الاحتياطي ZIP وحفظها...", 95)
            // 6. Write ZIP Package
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                    // A. Manifest
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestJson.toByteArray())
                    zos.closeEntry()

                    // B. Database Payload
                    val entryName = if (isEncrypted) "data.json.gz.enc" else "data.json.gz"
                    zos.putNextEntry(ZipEntry(entryName))
                    payloadFile.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()

                    // C. Attachments (Receipts)
                    if (includeAttachments) {
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
            }

            onProgress?.invoke("اكتملت عملية النسخ بنجاح!", 100)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun getRestorePreview(
        inputUri: Uri,
        password: CharArray?
    ): Result<RestorePreview> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "backup_restore_staging").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            // 1. Extract ZIP to Staging
            val resolver = context.contentResolver
            resolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val destFile = File(stagingDir, entry.name)
                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().use { output -> zis.copyTo(output) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            val manifestFile = File(stagingDir, "manifest.json")
            if (!manifestFile.exists()) {
                return@withContext Result.failure(Exception("ملف النسخ الاحتياطي غير صالح: لا يوجد ملف manifest.json"))
            }

            val manifestContent = manifestFile.readText()
            val manifest = parseManifest(manifestContent)

            // Validate format compatibility
            val isCompatible = manifest.schemaVersion <= currentSchemaVersion

            // Return preview
            if (manifest.isEncrypted) {
                if (password == null || password.isEmpty()) {
                    return@withContext Result.success(
                        RestorePreview(manifest.copy(isEncrypted = true), isCompatible, stagingDir.absolutePath)
                    )
                }

                // Verify Decryption and Checksum
                val encFile = File(stagingDir, "data.json.gz.enc")
                if (!encFile.exists()) {
                    return@withContext Result.failure(Exception("ملف النسخة الاحتياطية تالف: لا توجد بيانات قاعدة بيانات مشفرة."))
                }

                val calculatedChecksum = calculateSHA256(encFile)
                if (calculatedChecksum != manifest.checksumSHA256) {
                    return@withContext Result.failure(Exception("تنبيه أمان: تم تعديل ملف النسخة الاحتياطية (فشل فحص Checksum)!"))
                }

                try {
                    val rawEncBytes = encFile.readBytes()
                    val ivBytes = ByteArray(12)
                    System.arraycopy(rawEncBytes, 0, ivBytes, 0, 12)
                    val ciphertextBytes = ByteArray(rawEncBytes.size - 12)
                    System.arraycopy(rawEncBytes, 12, ciphertextBytes, 0, ciphertextBytes.size)

                    val key = CryptoUtils.deriveKey(password, android.util.Base64.decode(manifest.salt, android.util.Base64.NO_WRAP))
                    val decryptedBytes = CryptoUtils.decryptBytes(
                        android.util.Base64.encodeToString(ciphertextBytes, android.util.Base64.NO_WRAP),
                        android.util.Base64.encodeToString(ivBytes, android.util.Base64.NO_WRAP),
                        key
                    )
                    // Temporary save decrypted file to staging
                    val decryptedGzipFile = File(stagingDir, "data.json.gz")
                    decryptedGzipFile.writeBytes(decryptedBytes)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("كلمة المرور غير صحيحة أو الملف تالف."))
                }
            } else {
                val gzipFile = File(stagingDir, "data.json.gz")
                if (gzipFile.exists()) {
                    val calculatedChecksum = calculateSHA256(gzipFile)
                    if (calculatedChecksum != manifest.checksumSHA256) {
                        return@withContext Result.failure(Exception("تنبيه أمان: تم تعديل ملف النسخة الاحتياطية (فشل فحص Checksum)!"))
                    }
                }
            }

            Result.success(RestorePreview(manifest, isCompatible, stagingDir.absolutePath))
        } catch (e: Exception) {
            stagingDir.deleteRecursively()
            Result.failure(e)
        }
    }

    suspend fun performRestoreV2(
        preview: RestorePreview,
        selectedTables: List<String>?,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val stagingDir = File(preview.tempZipFile)
        var tempDbFile: File? = null
        var tempShmFile: File? = null
        var tempWalFile: File? = null

        val dbFile = context.getDatabasePath(dbName)
        val shmFile = File(dbFile.path + "-shm")
        val walFile = File(dbFile.path + "-wal")

        try {
            onProgress?.invoke("جاري استخراج وفحص ملفات النسخة الاحتياطية...", 15)
            // 1. Detect if Legacy ZIP or JSON V2
            val isLegacy = preview.manifest.schemaVersion == 1 && !File(stagingDir, "data.json.gz").exists()

            if (isLegacy) {
                // Execute Legacy ZIP Raw Restore
                val stagedDbFile = File(stagingDir, "database/app.db")
                val stagedShmFile = File(stagingDir, "database/app.db-shm")
                val stagedWalFile = File(stagingDir, "database/app.db-wal")

                validateStagedDatabase(stagedDbFile)

                onProgress?.invoke("إنشاء نقطة استعادة فيزيائية وقائية...", 40)
                // Create physical rollback point
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

                onProgress?.invoke("كتابة ملفات قاعدة البيانات القديمة...", 70)
                // Close database to overwrite files
                database.close()

                // Overwrite files
                stagedDbFile.copyTo(dbFile, overwrite = true)
                if (stagedShmFile.exists()) stagedShmFile.copyTo(shmFile, overwrite = true) else shmFile.delete()
                if (stagedWalFile.exists()) stagedWalFile.copyTo(walFile, overwrite = true) else walFile.delete()

                onProgress?.invoke("نقل المرفقات والإيصالات المسترجعة...", 90)
                // Copy attachments
                val stagedAttachmentsDir = File(stagingDir, "attachments")
                if (stagedAttachmentsDir.exists()) {
                    val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
                    stagedAttachmentsDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            file.copyTo(File(attachmentsDir, file.name), overwrite = true)
                        }
                    }
                }

                // Clean up temp backups
                tempDbFile?.delete()
                tempShmFile?.delete()
                tempWalFile?.delete()
            } else {
                // Execute New JSON V2 Restore
                if (preview.manifest.isEncrypted) {
                    val encFile = File(stagingDir, "data.json.gz.enc")
                    if (encFile.exists()) {
                        val calculatedChecksum = calculateSHA256(encFile)
                        if (calculatedChecksum != preview.manifest.checksumSHA256) {
                            throw Exception("تنبيه أمان: تم تعديل ملف النسخة الاحتياطية (فشل فحص Checksum)!")
                        }
                    }
                } else {
                    val gzipFile = File(stagingDir, "data.json.gz")
                    if (gzipFile.exists()) {
                        val calculatedChecksum = calculateSHA256(gzipFile)
                        if (calculatedChecksum != preview.manifest.checksumSHA256) {
                            throw Exception("تنبيه أمان: تم تعديل ملف النسخة الاحتياطية (فشل فحص Checksum)!")
                        }
                    }
                }

                val gzipFile = File(stagingDir, "data.json.gz")
                if (!gzipFile.exists()) {
                    throw Exception("ملف البيانات غير موجود داخل النسخة الاحتياطية.")
                }

                onProgress?.invoke("فك ضغط بيانات JSON (GZIP)...", 30)
                val decompressedJsonFile = File(stagingDir, "data.json")
                gzipDecompress(gzipFile, decompressedJsonFile)

                onProgress?.invoke("إنشاء نقطة استعادة فيزيائية وقائية...", 50)
                // Create physical rollback point
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

                onProgress?.invoke("بدء استيراد وتحديث جداول قاعدة البيانات المحددة...", 70)
                // Run JSON V2 restore transaction
                FileInputStream(decompressedJsonFile).use { fis ->
                    backupRepository.restoreBackupV2(fis, selectedTables)
                }

                onProgress?.invoke("تحديث ونقل الملفات المرفقة...", 90)
                // Copy attachments if present
                val stagedAttachmentsDir = File(stagingDir, "attachments")
                if (stagedAttachmentsDir.exists()) {
                    val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
                    stagedAttachmentsDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            file.copyTo(File(attachmentsDir, file.name), overwrite = true)
                        }
                    }
                }

                // Clean up temp backups
                tempDbFile?.delete()
                tempShmFile?.delete()
                tempWalFile?.delete()
            }

            onProgress?.invoke("اكتملت استعادة البيانات بنجاح!", 100)
            stagingDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            onProgress?.invoke("حدث خطأ! جاري التراجع واستعادة الحالة السابقة...", 95)
            // Restore database physically from rollback backups on failure
            try {
                database.close()
                tempDbFile?.let {
                    it.copyTo(dbFile, overwrite = true)
                    it.delete()
                }
                tempShmFile?.let {
                    it.copyTo(shmFile, overwrite = true)
                    it.delete()
                }
                tempWalFile?.let {
                    it.copyTo(walFile, overwrite = true)
                    it.delete()
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
            stagingDir.deleteRecursively()
            Result.failure(e)
        }
    }

    // --- local SAF Folder Backup integration ---

    suspend fun exportBackupToFolder(
        folderUri: Uri,
        password: CharArray?,
        includeAttachments: Boolean,
        maxKeepBackups: Int = 5,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<FileDetails> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("جاري التحقق من المجلد المختار...", 5)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
            val filename = "Qdash_Backup_${sdf.format(java.util.Date())}.zip"

            // Check space before starting
            onProgress?.invoke("التحقق من المساحة المتوفرة...", 7)
            val folderDocId = DocumentsContract.getTreeDocumentId(folderUri)
            val folderDocumentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, folderDocId)
            
            // Check free space on cache or system if possible (external directory size isn't directly queryable, but cache is a proxy)
            val freeBytes = context.cacheDir.freeSpace
            if (freeBytes < 20 * 1024 * 1024) { // Needs at least 20MB free
                return@withContext Result.failure(Exception("المساحة المتوفرة في جهازك غير كافية لإنشاء نسخة احتياطية."))
            }

            // Create file inside folder tree
            val fileUri = createDocumentInTree(context, folderUri, filename, "application/zip")
                ?: return@withContext Result.failure(Exception("تعذر إنشاء ملف النسخة الاحتياطية داخل المجلد المختار. يرجى التحقق من أذونات الوصول."))

            onProgress?.invoke("بدء تصدير البيانات...", 10)

            val res = exportBackupV2(fileUri, password, includeAttachments, onProgress)
            if (res.isSuccess) {
                val size = try {
                    context.contentResolver.openFileDescriptor(fileUri, "r")?.use { it.statSize } ?: 0L
                } catch (e: Exception) {
                    0L
                }

                // Cleanup older backups
                onProgress?.invoke("جاري تنظيف وتدوير النسخ الاحتياطية القديمة...", 98)
                cleanupOldBackups(context, folderUri, maxKeepBackups)

                onProgress?.invoke("اكتمل حفظ وتصدير النسخة الاحتياطية بنجاح!", 100)
                Result.success(FileDetails(filename, size, fileUri.toString()))
            } else {
                try {
                    DocumentsContract.deleteDocument(context.contentResolver, fileUri)
                } catch (e: Exception) {}
                Result.failure(res.exceptionOrNull() ?: Exception("فشل تصدير البيانات."))
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("فشلت الصلاحية: لم يعد للبرنامج حق الوصول للمجلد المحدد. يرجى إعادة اختياره."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isFolderUriValid(uriString: String?): Boolean {
        if (uriString.isNullOrEmpty()) return false
        val uri = Uri.parse(uriString)
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        val hasPersisted = persistedUriPermissions.any { 
            it.uri.toString() == uriString && (it.isReadPermission || it.isWritePermission) 
        }
        if (!hasPersisted) return false

        return try {
            val documentId = DocumentsContract.getTreeDocumentId(uri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
            context.contentResolver.query(parentDocumentUri, null, null, null, null)?.use {
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun createDocumentInTree(context: Context, treeUri: Uri, displayName: String, mimeType: String): Uri? {
        return try {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            DocumentsContract.createDocument(context.contentResolver, parentDocumentUri, mimeType, displayName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanupOldBackups(context: Context, treeUri: Uri, maxKeep: Int) {
        try {
            val resolver = context.contentResolver
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            val files = mutableListOf<BackupFileInfo>()
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modifiedIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)
                    if (name.startsWith("Qdash_Backup_") && name.endsWith(".zip")) {
                        val docId = cursor.getString(idIdx)
                        val lastModified = cursor.getLong(modifiedIdx)
                        files.add(BackupFileInfo(docId, name, lastModified))
                    }
                }
            }
            if (files.size > maxKeep) {
                files.sortBy { it.lastModified }
                val toDeleteCount = files.size - maxKeep
                for (i in 0 until toDeleteCount) {
                    val fileToDelete = files[i]
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, fileToDelete.docId)
                    DocumentsContract.deleteDocument(resolver, docUri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Legacy ZIP Export/Import APIs for backward compatibility ---

    suspend fun exportBackup(outputUri: Uri): Result<Unit> = exportBackupV2(outputUri, null, true)

    suspend fun importBackup(inputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        getRestorePreview(inputUri, null).fold(
            onSuccess = { preview ->
                performRestoreV2(preview, null)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    // --- Helpers ---

    private fun gzipCompress(source: File, target: File) {
        java.util.zip.GZIPOutputStream(FileOutputStream(target)).use { gzip ->
            source.inputStream().use { input ->
                input.copyTo(gzip)
            }
        }
    }

    private fun gzipDecompress(source: File, target: File) {
        java.util.zip.GZIPInputStream(FileInputStream(source)).use { gzip ->
            target.outputStream().use { output ->
                gzip.copyTo(output)
            }
        }
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(4096)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun generateManifestV2(
        isEncrypted: Boolean,
        salt: ByteArray?,
        checksum: String,
        recordCounts: Map<String, Int>
    ): String {
        val appVersionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        val json = JSONObject().apply {
            put("schemaVersion", currentSchemaVersion)
            put("appVersion", appVersionName)
            put("createdAt", System.currentTimeMillis())
            put("isEncrypted", isEncrypted)
            put("salt", salt?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } ?: JSONObject.NULL)
            put("checksumSHA256", checksum)
            
            val countsJson = JSONObject()
            recordCounts.forEach { (table, count) ->
                countsJson.put(table, count)
            }
            put("recordCounts", countsJson)
        }
        return json.toString(4)
    }

    private fun parseManifest(content: String): BackupManifestV2 {
        val json = JSONObject(content)
        val schemaVer = json.optInt("schemaVersion", legacyBackupFormatVersion)

        if (schemaVer == legacyBackupFormatVersion) {
            val dbVer = json.optInt("dbVersion", 1)
            val appVer = json.optString("appVersion", "1.0")
            val createdAt = json.optLong("createdAt", System.currentTimeMillis())
            return BackupManifestV2(
                schemaVersion = legacyBackupFormatVersion,
                appVersion = appVer,
                createdAt = createdAt,
                isEncrypted = false,
                salt = null,
                checksumSHA256 = null,
                recordCounts = emptyMap()
            )
        }

        val appVer = json.getString("appVersion")
        val createdAt = json.getLong("createdAt")
        val isEncrypted = json.getBoolean("isEncrypted")
        val salt = if (json.isNull("salt")) null else json.getString("salt")
        val checksum = json.getString("checksumSHA256")

        val recordCounts = mutableMapOf<String, Int>()
        val countsObj = json.getJSONObject("recordCounts")
        countsObj.keys().forEach { table ->
            recordCounts[table] = countsObj.getInt(table)
        }

        return BackupManifestV2(
            schemaVersion = schemaVer,
            appVersion = appVer,
            createdAt = createdAt,
            isEncrypted = isEncrypted,
            salt = salt,
            checksumSHA256 = checksum,
            recordCounts = recordCounts
        )
    }

    private suspend fun getRecordCounts(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        try {
            counts["accounts"] = database.accountDao().getAllAccountsIncludingArchived().first().size
            counts["categories"] = database.categoryDao().getAllCategories().first().filter { !it.isSystem }.size
            counts["transactions"] = database.transactionDao().getAllTransactions().first().size
            counts["income_sources"] = database.incomeSourceDao().getAllIncomeSources().first().size
            counts["saving_goals"] = database.savingGoalDao().getAllSavingGoals().first().size
            counts["savings_contributions"] = database.savingsContributionDao().getAllContributions().first().size
            counts["subscriptions"] = database.subscriptionDao().getAllSubscriptions().first().size
            counts["debts"] = database.debtDao().getAllDebts().first().size
            counts["debt_payments"] = database.debtPaymentDao().getAllPayments().first().size
            counts["transfers"] = database.transferDao().getAllTransfers().first().size
            counts["budget_goals"] = database.budgetGoalDao().getAllBudgetGoals().first().size
            counts["financial_plans"] = database.financialPlanDao().getAllPlans().first().size
            counts["transaction_templates"] = database.transactionTemplateDao().getAllTemplates().first().size
            counts["notifications"] = database.notificationDao().getAllNotifications().first().size
            counts["category_rules"] = database.categoryRuleDao().getAllRules().first().size
            counts["user_category_mappings"] = database.userCategoryMappingDao().getAllMappings().first().size
            counts["ai_chat_messages"] = database.aiChatDao().getAllMessagesOnce().size
            counts["postal_profiles"] = database.postalProfileDao().getAllProfiles().first().size
            counts["salary_delays"] = database.salaryDelayDao().getAllSalaryDelaysOnce().size
            counts["salary_distributions"] = database.salaryDistributionDao().getAllDistributionsOnce().size
            counts["salary_envelopes"] = database.salaryDistributionDao().getAllEnvelopesOnce().size
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return counts
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
            listOf("accounts", "categories", "transactions").forEach { table ->
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
                    require(cursor.moveToFirst()) { "النسخة الاحتياطية تفتقد جدول $table." }
                }
            }
        }
    }

    private data class BackupFileInfo(val docId: String, val name: String, val lastModified: Long)
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
