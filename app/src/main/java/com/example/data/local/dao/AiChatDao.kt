package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.AiChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_messages WHERE sessionTitle = :sessionTitle ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionTitle: String): Flow<List<AiChatMessageEntity>>

    @Query("SELECT DISTINCT sessionTitle FROM ai_chat_messages ORDER BY timestamp DESC")
    fun getAllSessionTitles(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity): Long

    @Query("DELETE FROM ai_chat_messages WHERE sessionTitle = :sessionTitle")
    suspend fun clearHistory(sessionTitle: String)

    @Query("DELETE FROM ai_chat_messages WHERE sessionTitle = :sessionTitle")
    suspend fun deleteSession(sessionTitle: String)
}
