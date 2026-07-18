package com.qdash.domain.repository

import android.content.Context
import com.qdash.domain.model.BackupFileMetadata
import kotlinx.coroutines.flow.StateFlow

interface DriveSyncRepository {
    suspend fun uploadToAppData(context: Context): Result<Unit>
    suspend fun downloadFromAppData(context: Context): Result<Boolean>
    suspend fun checkIfBackupExists(context: Context): Result<BackupFileMetadata?>
    
    val backupFoundToRestore: StateFlow<BackupFileMetadata?>
    fun setBackupFoundToRestore(metadata: BackupFileMetadata?)
}
