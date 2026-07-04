package com.example.domain.model

import java.io.Serializable

data class BackupManifestV2(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val isEncrypted: Boolean,
    val salt: String?,
    val checksumSHA256: String?,
    val recordCounts: Map<String, Int>
) : Serializable

data class RestorePreview(
    val manifest: BackupManifestV2,
    val isCompatible: Boolean,
    val tempZipFile: String
) : Serializable
