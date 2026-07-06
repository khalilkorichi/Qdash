package com.qdash.domain.repository

import android.net.Uri
import com.qdash.domain.model.BackupFileInfo
import com.qdash.domain.model.RestorePreview
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {
    suspend fun exportAllDataAsJson(): JSONObject
    suspend fun restoreFromJson(json: JSONObject)

    // New V2 Streaming APIs
    suspend fun exportBackupV2(outputStream: OutputStream, selectedTables: List<String>? = null): Map<String, Int>
    suspend fun restoreBackupV2(inputStream: InputStream, selectedTables: List<String>? = null)

    // APIs added to support ARCH-001 (removing BackupManager from UI/VMs)
    fun isFolderUriValid(uriString: String?): Boolean
    
    suspend fun exportBackupToFolder(
        folderUri: Uri,
        pwd: CharArray?,
        includeAttachments: Boolean,
        maxKeepBackups: Int,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<BackupFileInfo>
    
    suspend fun exportBackupV2(
        outputUri: Uri,
        pwd: CharArray?,
        includeAttachments: Boolean,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<Unit>

    suspend fun getRestorePreview(
        inputUri: Uri,
        pwd: CharArray?
    ): Result<RestorePreview>

    suspend fun performRestoreV2(
        preview: RestorePreview,
        selectedTables: List<String>?,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<Unit>

    suspend fun exportBackup(uri: Uri): Result<Unit>

    // APIs added to support ARCH-003 (removing main-thread File operations / SharedPreferences from VM)
    suspend fun backupLocalJsonData(): Result<String>
    suspend fun restoreLocalJsonData(): Result<Unit>
    suspend fun hasLocalJsonBackup(): Boolean
    suspend fun resetAllData(): Result<Unit>
    
    // Backup Scheduling API to isolate WorkManager from VMs
    fun updateBackupSchedule(interval: String)
}
