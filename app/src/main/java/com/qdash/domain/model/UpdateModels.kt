package com.qdash.domain.model

import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val updateIdentity: Long,
    val apkUrl: String,
    val apkSize: Long,
    val apkSha256: String,
    val mandatory: Boolean,
    val releaseNotes: String?
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val speed: String, val eta: String, val info: UpdateInfo) : DownloadState()
    data class Paused(val progress: Int, val info: UpdateInfo) : DownloadState()
    data class Success(val file: File, val info: UpdateInfo) : DownloadState()
    data class Error(val message: String, val info: UpdateInfo) : DownloadState()
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
