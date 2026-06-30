package com.example.domain.repository

import com.example.domain.model.AiChatMessage
import com.example.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

data class AiResponse(
    val replyText: String,
    val draftTransaction: Transaction? = null,
    val categoryName: String? = null,
    val accountName: String? = null
)

interface AiRepository {
    suspend fun generateResponse(prompt: String, modelId: String): AiResponse
    suspend fun getInitialSuggestions(): List<String>

    fun getMessagesBySession(sessionTitle: String): Flow<List<AiChatMessage>>
    suspend fun insertMessage(message: AiChatMessage): Long
    suspend fun clearHistory(sessionTitle: String)
    suspend fun generateAiResponse(sessionTitle: String, userPrompt: String, modelId: String): String

    fun getAllSessionTitles(): Flow<List<String>>
    suspend fun deleteSession(sessionTitle: String)
    suspend fun sendCardMessage(systemPrompt: String, history: List<AiChatMessage>, userMessage: String): String
}
