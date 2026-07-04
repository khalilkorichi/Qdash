package com.qdash.domain.repository

import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {
    suspend fun exportAllDataAsJson(): JSONObject
    suspend fun restoreFromJson(json: JSONObject)

    // New V2 Streaming APIs
    suspend fun exportBackupV2(outputStream: OutputStream, selectedTables: List<String>? = null): Map<String, Int>
    suspend fun restoreBackupV2(inputStream: InputStream, selectedTables: List<String>? = null)
}
