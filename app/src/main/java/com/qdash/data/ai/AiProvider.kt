package com.qdash.data.ai

import com.qdash.domain.model.AiChatMessage

interface AiProvider {
    val name: String
    suspend fun sendCardMessage(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): Result<String>
}
