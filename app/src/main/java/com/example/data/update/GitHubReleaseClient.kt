package com.example.data.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class UpdateManifest(
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "versionName") val versionName: String,
    @Json(name = "updateIdentity") val updateIdentity: Long,
    @Json(name = "publishedAt") val publishedAt: String,
    @Json(name = "apkUrl") val apkUrl: String,
    @Json(name = "apkSize") val apkSize: Long,
    @Json(name = "apkSha256") val apkSha256: String,
    @Json(name = "mandatory") val mandatory: Boolean,
    @Json(name = "releaseNotes") val releaseNotes: String?
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String?,
    @Json(name = "published_at") val publishedAt: String,
    @Json(name = "body") val body: String?,
    @Json(name = "assets") val assets: List<GitHubAsset>
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    @Json(name = "name") val name: String,
    @Json(name = "size") val size: Long,
    @Json(name = "browser_download_url") val browserDownloadUrl: String
)

interface GitHubReleaseClient {
    @GET
    suspend fun fetchUpdateManifest(@Url url: String): UpdateManifest

    @GET("repos/khalilkorichi/Qdash/releases/latest")
    suspend fun fetchLatestRelease(): GitHubReleaseResponse
}
