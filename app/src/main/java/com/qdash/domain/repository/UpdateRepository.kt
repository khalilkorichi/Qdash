package com.qdash.domain.repository

import android.net.Uri
import com.qdash.domain.model.CheckingStep
import com.qdash.domain.model.DownloadState
import com.qdash.domain.model.UpdateInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

interface UpdateRepository {
    val downloadState: Flow<DownloadState>
    fun startDownload(info: UpdateInfo)
    fun pauseDownload(info: UpdateInfo)
    fun cancelDownload(info: UpdateInfo)

    suspend fun checkForUpdates(onStep: suspend (CheckingStep) -> Unit = {}): Result<UpdateInfo>
    fun downloadApk(url: String, startBytes: Long = 0L): Flow<DownloadState>
    fun verifyApkSha256(file: File, expectedSha256: String): Boolean
    suspend fun copyApkToDownloads(file: File, filename: String): Uri?
    suspend fun backupDataBeforeUpdate(): Result<Uri>
    suspend fun saveDownloadedApk(file: File, versionName: String): File
    suspend fun getDownloadedApks(): List<File>
    suspend fun deleteDownloadedApk(file: File): Boolean
}
