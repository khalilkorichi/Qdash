package com.example.domain.repository

import org.json.JSONObject

/**
 * BackupRepository isolates all SQLite database JSON serialization
 * and deserialization tasks from settings ViewModel.
 */
interface BackupRepository {
    suspend fun exportAllDataAsJson(): JSONObject
    suspend fun restoreFromJson(json: JSONObject)
}
