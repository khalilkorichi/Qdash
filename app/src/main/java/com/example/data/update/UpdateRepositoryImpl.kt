package com.example.data.update

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.backup.BackupManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    private val context: Context
) : UpdateRepository {

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

    override suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. Try to fetch custom update.json manifest from raw GitHub CDN
            val manifestUrl = "https://raw.githubusercontent.com/khalilkorichi/Qdash/main/update.json"
            val manifest = try {
                client.fetchUpdateManifest(manifestUrl)
            } catch (e: Exception) {
                null
            }

            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val localBuildTime = BuildConfig.BUILD_TIMESTAMP
            val localVersionName = BuildConfig.VERSION_NAME

            if (manifest != null) {
                val remoteTime = try {
                    format.parse(manifest.publishedAt)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
                val localIdentity = BuildConfig.UPDATE_IDENTITY
                val remoteIdentity = manifest.updateIdentity
                val remoteVersionName = manifest.versionName

                val isNewerVersion = isVersionNewer(localVersionName, remoteVersionName)
                val isSameVersionAndNewerIdentity = (remoteVersionName.removePrefix("v").trim() == localVersionName.removePrefix("v").trim()) && (remoteIdentity > localIdentity)
                val isNewerBuildDate = remoteTime > localBuildTime && (remoteVersionName.removePrefix("v").trim() == localVersionName.removePrefix("v").trim())

                val hasUpdate = isNewerVersion || isSameVersionAndNewerIdentity || isNewerBuildDate
                
                return@withContext Result.success(
                    UpdateInfo(
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
                )
            }

            // 2. Fallback to GitHub Releases API if update.json doesn't exist/fails
            val latestRelease = client.fetchLatestRelease()
            val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext Result.failure(Exception("لم يتم العثور على ملف APK في إصدارات GitHub."))

            val remoteTagName = latestRelease.tagName
            val cleanTagName = remoteTagName.removePrefix("v").trim()
            
            val remoteTime = try {
                format.parse(latestRelease.publishedAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }

            val isNewerVersion = isVersionNewer(localVersionName, cleanTagName)
            val isNewerBuildDate = remoteTime > localBuildTime && (cleanTagName == localVersionName.removePrefix("v").trim())

            val hasUpdate = isNewerVersion || isNewerBuildDate

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    versionCode = 1, // Default fallback
                    versionName = cleanTagName,
                    updateIdentity = remoteTime,
                    apkUrl = apkAsset.browserDownloadUrl,
                    apkSize = apkAsset.size,
                    apkSha256 = null,
                    mandatory = false,
                    releaseNotes = latestRelease.body
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun downloadApk(url: String, startBytes: Long): Flow<DownloadState> = flow {
        emit(DownloadState.Progress(if (startBytes > 0L) -2 else 0))
        try {
            val requestBuilder = Request.Builder().url(url)
            if (startBytes > 0L) {
                requestBuilder.addHeader("Range", "bytes=$startBytes-")
            }
            val request = requestBuilder.build()
            val response = downloadHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw java.io.IOException("فشل تحميل الملف: ${response.message}")
            }
            val body = response.body ?: throw java.io.IOException("محتوى الملف فارغ.")
            val remainingLength = body.contentLength()
            val totalLength = if (startBytes > 0L && (response.code == 206 || response.code == 200)) {
                if (response.code == 206) remainingLength + startBytes else remainingLength
            } else {
                remainingLength
            }
            
            val tempFile = File(context.cacheDir, "Qdash-update-temp.apk")
            val isAppend = startBytes > 0L && response.code == 206
            
            body.byteStream().use { input ->
                FileOutputStream(tempFile, isAppend).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastEmittedProgress = 0
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        val currentTotalRead = totalBytesRead + (if (isAppend) startBytes else 0L)
                        if (totalLength > 0) {
                            val progress = ((currentTotalRead * 100) / totalLength).toInt()
                            if (progress > lastEmittedProgress) {
                                emit(DownloadState.Progress(progress))
                                lastEmittedProgress = progress
                            }
                        } else {
                            emit(DownloadState.Progress(-1))
                        }
                    }
                }
            }
            emit(DownloadState.Success(tempFile))
        } catch (e: Exception) {
            emit(DownloadState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun verifyApkSha256(file: File, expectedSha256: String): Boolean {
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
            hex.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
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

    override suspend fun backupDataBeforeUpdate(backupManager: BackupManager): Result<Uri> = withContext(Dispatchers.IO) {
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
