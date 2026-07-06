package com.qdash.domain.usecase.update

import android.net.Uri
import com.qdash.domain.model.DownloadState
import com.qdash.domain.model.UpdateInfo
import com.qdash.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadUpdateUseCase(private val updateRepository: UpdateRepository) {
    val downloadState: Flow<DownloadState>
        get() = updateRepository.downloadState

    fun startDownload(info: UpdateInfo) {
        updateRepository.startDownload(info)
    }

    fun pauseDownload(info: UpdateInfo) {
        updateRepository.pauseDownload(info)
    }

    fun cancelDownload(info: UpdateInfo) {
        updateRepository.cancelDownload(info)
    }

    suspend fun getDownloadedApks(): List<File> {
        return updateRepository.getDownloadedApks()
    }

    suspend fun deleteDownloadedApk(file: File): Boolean {
        return updateRepository.deleteDownloadedApk(file)
    }

    suspend fun backupDataBeforeUpdate(): Result<Uri> {
        return updateRepository.backupDataBeforeUpdate()
    }
    
    suspend fun copyApkToDownloads(file: File, filename: String): Uri? {
        return updateRepository.copyApkToDownloads(file, filename)
    }
}
