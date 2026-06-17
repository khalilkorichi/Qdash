package com.example.data.update

import android.net.Uri
import com.example.data.backup.BackupManager
import kotlinx.coroutines.flow.Flow
import java.io.File

data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val updateIdentity: Long,
    val apkUrl: String,
    val apkSize: Long,
    val apkSha256: String?,
    val mandatory: Boolean,
    val releaseNotes: String?
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val percentage: Int) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val throwable: Throwable) : DownloadState()
}

sealed class CheckingStep {
    object Idle : CheckingStep()
    object ReadingLocalVersion : CheckingStep()
    object FetchingManifest : CheckingStep()
    object FetchingReleaseFallback : CheckingStep()
    object ComparingVersions : CheckingStep()
    data class Success(val info: UpdateInfo) : CheckingStep()
    data class Error(val message: String) : CheckingStep()
}

interface UpdateRepository {
    suspend fun checkForUpdates(onStep: suspend (CheckingStep) -> Unit = {}): Result<UpdateInfo>
    fun downloadApk(url: String, startBytes: Long = 0L): Flow<DownloadState>
    fun verifyApkSha256(file: File, expectedSha256: String): Boolean
    suspend fun copyApkToDownloads(file: File, filename: String): Uri?
    suspend fun backupDataBeforeUpdate(backupManager: BackupManager): Result<Uri>
    suspend fun saveDownloadedApk(file: File, versionName: String): File
    suspend fun getDownloadedApks(): List<File>
    suspend fun deleteDownloadedApk(file: File): Boolean
}
