package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.TransactionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TransactionTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TransactionTemplateEntity)

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT * FROM transaction_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TransactionTemplateEntity?

    @Query("SELECT * FROM transaction_templates ORDER BY isPinned DESC, usageCount DESC, name ASC")
    fun getAllTemplates(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE isPinned = 1 ORDER BY usageCount DESC LIMIT 4")
    fun getPinnedTemplates(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates ORDER BY usageCount DESC, lastUsedAt DESC LIMIT 10")
    fun getFrequentTemplates(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTemplates(query: String): Flow<List<TransactionTemplateEntity>>

    @Query("UPDATE transaction_templates SET usageCount = usageCount + 1, lastUsedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)

    @Query("UPDATE transaction_templates SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :id")
    suspend fun togglePin(id: Long, isPinned: Boolean, timestamp: Long)
}
