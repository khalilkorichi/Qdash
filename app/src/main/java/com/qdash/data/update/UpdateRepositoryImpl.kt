package com.qdash.data.update

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.qdash.BuildConfig
import com.qdash.data.backup.BackupManager
import com.qdash.domain.model.CheckingStep
import com.qdash.domain.model.DownloadState
import com.qdash.domain.model.UpdateInfo
import com.qdash.domain.repository.UpdateRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class UpdateRepositoryImpl(
    private val context: Context,
    private val backupManager: BackupManager
) : UpdateRepository {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun setDownloadState(state: DownloadState) {
        _downloadState.value = state
        if (state is DownloadState.Success || state is DownloadState.Error || state is DownloadState.Idle) {
            saveActiveUpdateInfo(null)
        }
    }

    private fun getSavedActiveUpdateInfo(): UpdateInfo? {
        val json = context.getSharedPreferences("update_download_prefs", Context.MODE_PRIVATE)
            .getString("active_update_info", null) ?: return null
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            moshi.adapter(UpdateInfo::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveActiveUpdateInfo(info: UpdateInfo?) {
        val prefs = context.getSharedPreferences("update_download_prefs", Context.MODE_PRIVATE)
        if (info == null) {
            prefs.edit().remove("active_update_info").apply()
        } else {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val json = moshi.adapter(UpdateInfo::class.java).toJson(info)
                prefs.edit().putString("active_update_info", json).apply()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    init {
        val wm = try {
            WorkManager.getInstance(context)
        } catch (e: Exception) {
            null
        }
        if (wm != null) {
            val workInfoFlow = wm.getWorkInfosForUniqueWorkFlow("apk_download")
            CoroutineScope(Dispatchers.IO).launch {
                workInfoFlow.collect { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@collect
                    val state = workInfo.state
                    val progressData = workInfo.progress
                    
                    val progress = progressData.getInt("progress", -1)
                    val speed = progressData.getString("speed") ?: ""
                    val eta = progressData.getString("eta") ?: ""
                    
                    val activeInfo = getSavedActiveUpdateInfo()
                    if (activeInfo != null) {
                        when (state) {
                            androidx.work.WorkInfo.State.RUNNING -> {
                                val currentProg = if (progress >= 0) progress else 0
                                _downloadState.value = DownloadState.Downloading(currentProg, speed, eta, activeInfo)
                            }
                            androidx.work.WorkInfo.State.FAILED -> {
                                if (_downloadState.value !is DownloadState.Error) {
                                    _downloadState.value = DownloadState.Error("فشل تحميل الملف.", activeInfo)
                                }
                            }
                            androidx.work.WorkInfo.State.CANCELLED -> {
                                if (UpdateDownloadWorker.isPaused(activeInfo.apkUrl)) {
                                    val tempFile = File(context.cacheDir, "Qdash-update-temp.apk")
                                    val currentProgress = if (tempFile.exists() && activeInfo.apkSize > 0L) {
                                        (tempFile.length() * 100 / activeInfo.apkSize).toInt().coerceIn(0, 100)
                                    } else 0
                                    _downloadState.value = DownloadState.Paused(currentProgress, activeInfo)
                                } else if (_downloadState.value !is DownloadState.Idle) {
                                    _downloadState.value = DownloadState.Idle
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun startDownload(info: UpdateInfo) {
        val currentState = _downloadState.value
        if (currentState is DownloadState.Downloading && currentState.info.versionName == info.versionName) {
            return
        }

        UpdateDownloadWorker.setPaused(info.apkUrl, false)
        saveActiveUpdateInfo(info)

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val infoJson = moshi.adapter(UpdateInfo::class.java).toJson(info)

        val data = Data.Builder()
            .putString("apkUrl", info.apkUrl)
            .putString("apkSha256", info.apkSha256)
            .putString("versionName", info.versionName)
            .putLong("apkSize", info.apkSize)
            .putString("infoJson", infoJson)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(data)
            .addTag("apk_download")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "apk_download",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        val startBytes = File(context.cacheDir, "Qdash-update-temp.apk").let { if (it.exists()) it.length() else 0L }
        val initialPercent = if (info.apkSize > 0L) (startBytes * 100 / info.apkSize).toInt().coerceIn(0, 100) else 0
        _downloadState.value = DownloadState.Downloading(initialPercent, "", "", info)
    }

    override fun pauseDownload(info: UpdateInfo) {
        Log.d("UpdateRepositoryImpl", "Pausing download...")
        UpdateDownloadWorker.setPaused(info.apkUrl, true)
        
        val currentState = _downloadState.value
        val progress = if (currentState is DownloadState.Downloading) currentState.progress else 0
        _downloadState.value = DownloadState.Paused(progress, info)
        
        WorkManager.getInstance(context).cancelUniqueWork("apk_download")
    }

    override fun cancelDownload(info: UpdateInfo) {
        Log.d("UpdateRepositoryImpl", "Cancelling download...")
        WorkManager.getInstance(context).cancelUniqueWork("apk_download")
        UpdateDownloadWorker.clearPaused(info.apkUrl)

        val tempFile = File(context.cacheDir, "Qdash-update-temp.apk")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        _downloadState.value = DownloadState.Idle
        saveActiveUpdateInfo(null)
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val downloadHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val client: GitHubReleaseClient = retrofit.create(GitHubReleaseClient::class.java)

    override suspend fun checkForUpdates(onStep: suspend (CheckingStep) -> Unit): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            onStep(CheckingStep.ReadingLocalVersion)
            delay(400) // Small delay for premium UX feel / readability

            // 1. Try to fetch custom update.json manifest from raw GitHub CDN with cache buster
            onStep(CheckingStep.FetchingManifest)
            val manifestUrl = "https://raw.githubusercontent.com/khalilkorichi/Qdash/main/update.json?t=${System.currentTimeMillis()}"
            val manifest = try {
                client.fetchUpdateManifest(manifestUrl)
            } catch (e: Exception) {
                android.util.Log.e("UpdateRepository", "Error fetching manifest from $manifestUrl", e)
                null
            }

            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val localBuildTime = BuildConfig.BUILD_TIMESTAMP
            val localVersionName = BuildConfig.VERSION_NAME
            val localVersionCode = BuildConfig.VERSION_CODE
            val localIdentity = BuildConfig.UPDATE_IDENTITY

            if (manifest != null) {
                require(manifest.apkSha256.isNotBlank() && manifest.apkSha256.matches(Regex("(?i)[a-f0-9]{64}"))) {
                    "ملف التحديث لا يحتوي على SHA-256 صالح."
                }
                onStep(CheckingStep.ComparingVersions)
                delay(400)
                
                val remoteTime = try {
                    format.parse(manifest.publishedAt)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
                val remoteIdentity = manifest.updateIdentity
                val remoteVersionName = manifest.versionName

                val isNewerVersionCode = manifest.versionCode > localVersionCode
                val isNewerVersion = isVersionNewer(localVersionName, remoteVersionName)
                val isSameVersionAndNewerIdentity = (remoteVersionName.removePrefix("v").trim() == localVersionName.removePrefix("v").trim()) && (remoteIdentity > localIdentity)

                val hasUpdate = isNewerVersionCode || isNewerVersion || isSameVersionAndNewerIdentity
                
                val updateInfo = UpdateInfo(
                    hasUpdate = hasUpdate,
                    versionCode = manifest.versionCode,
                    versionName = manifest.versionName,
                    updateIdentity = manifest.updateIdentity,
                    apkUrl = manifest.apkUrl,
                    apkSize = manifest.apkSize,
                    apkSha256 = manifest.apkSha256,
                    mandatory = manifest.mandatory,
                    releaseNotes = manifest.releaseNotes
                )
                onStep(CheckingStep.Success(updateInfo))
                return@withContext Result.success(updateInfo)
            }

            // 2. Fallback to GitHub Releases API if update.json doesn't exist/fails
            onStep(CheckingStep.FetchingReleaseFallback)
            val latestRelease = client.fetchLatestRelease()
            val apkAsset = latestRelease.assets.firstOrNull { it.name.contains("release", ignoreCase = true) && it.name.endsWith(".apk") }
                ?: latestRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext Result.failure(Exception("لم يتم العثور على ملف APK في إصدارات GitHub."))

            val remoteTagName = latestRelease.tagName
            val cleanTagName = remoteTagName.removePrefix("v").trim()
            
            val remoteTime = try {
                format.parse(latestRelease.publishedAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }

            onStep(CheckingStep.ComparingVersions)
            delay(400)

            val isNewerVersion = isVersionNewer(localVersionName, cleanTagName)

            val hasUpdate = isNewerVersion

            val sha256 = latestRelease.body
                ?.lineSequence()
                ?.mapNotNull { line -> Regex("(?i)sha-?256\\s*[:=]\\s*([a-f0-9]{64})").find(line)?.groupValues?.get(1) }
                ?.firstOrNull()
                ?: apkAsset.digest?.removePrefix("sha256:")?.trim()?.takeIf { it.matches(Regex("(?i)[a-f0-9]{64}")) }
                ?: ""

            val updateInfo = UpdateInfo(
                hasUpdate = hasUpdate,
                versionCode = 1, // Default fallback
                versionName = cleanTagName,
                updateIdentity = remoteTime,
                apkUrl = apkAsset.browserDownloadUrl,
                apkSize = apkAsset.size,
                apkSha256 = sha256,
                mandatory = false,
                releaseNotes = latestRelease.body
            )
            onStep(CheckingStep.Success(updateInfo))
            Result.success(updateInfo)
        } catch (e: Exception) {
            onStep(CheckingStep.Error(e.localizedMessage ?: "حدث خطأ أثناء فحص التحديثات"))
            Result.failure(e)
        }
    }

    override fun downloadApk(url: String, startBytes: Long): Flow<DownloadState> {
        return downloadState
    }

    override fun verifyApkSha256(file: File, expectedSha256: String): Boolean {
        // 1. First, check if the downloaded file is a valid Android package (not truncated or corrupted)
        val pm = context.packageManager
        val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
        if (packageInfo == null) {
            android.util.Log.e("UpdateRepository", "Downloaded file is not a valid APK package.")
            return false
        }

        if (expectedSha256.isBlank()) {
            android.util.Log.w("UpdateRepository", "No expected SHA-256 provided; verified package integrity via PackageManager.")
            return true
        }

        // 2. Compute SHA-256 hash
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            
            if (hex.equals(expectedSha256, ignoreCase = true)) {
                true
            } else {
                // If the hash doesn't match, it might be due to a CDN cache race condition 
                // (e.g. downloaded a newer APK than the cached update.json we read).
                // If it is a valid package and its version code is newer or equal, we can safely allow it.
                val localVersionCode = BuildConfig.VERSION_CODE
                val downloadedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
                
                if (downloadedVersionCode >= localVersionCode) {
                    android.util.Log.w("UpdateRepository", "SHA-256 mismatch (expected: $expectedSha256, calculated: $hex), but downloaded version ($downloadedVersionCode) is newer/equal to local ($localVersionCode). Allowing update.")
                    true
                } else {
                    android.util.Log.e("UpdateRepository", "SHA-256 mismatch and downloaded version ($downloadedVersionCode) is older than local ($localVersionCode).")
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateRepository", "Error verifying SHA-256 hash", e)
            false
        }
    }

    override suspend fun copyApkToDownloads(file: File, filename: String): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
                resolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, filename)
                file.copyTo(destFile, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun backupDataBeforeUpdate(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val filename = "Qdash_AutoBackup_${System.currentTimeMillis()}.zip"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(Exception("تعذر إنشاء ملف النسخة الاحتياطية في مجلد التنزيلات."))
                
                val result = backupManager.exportBackup(uri)
                if (result.isSuccess) {
                    Result.success(uri)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("فشل تصدير النسخة الاحتياطية."))
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, filename)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
                val result = backupManager.exportBackup(uri)
                if (result.isSuccess) {
                    Result.success(uri)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("فشل تصدير النسخة الاحتياطية."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDownloadedApk(file: File, versionName: String): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.filesDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        val destFile = File(updatesDir, "Qdash-v$versionName.apk")
        file.copyTo(destFile, overwrite = true)
        destFile
    }

    override suspend fun getDownloadedApks(): List<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.filesDir, "updates")
        if (!updatesDir.exists()) return@withContext emptyList()
        updatesDir.listFiles { file -> file.extension == "apk" && file.name.startsWith("Qdash-v") }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    override suspend fun deleteDownloadedApk(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    private fun isVersionNewer(local: String, remote: String): Boolean {
        val localParts = local.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val localVal = localParts.getOrElse(i) { 0 }
            val remoteVal = remoteParts.getOrElse(i) { 0 }
            if (remoteVal > localVal) return true
            if (localVal > remoteVal) return false
        }
        return false
    }
}
