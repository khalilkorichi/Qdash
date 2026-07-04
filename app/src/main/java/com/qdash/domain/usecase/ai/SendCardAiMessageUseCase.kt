package com.qdash.domain.usecase.ai

import com.qdash.domain.model.AiChatMessage
import com.qdash.domain.repository.AiRepository

class SendCardAiMessageUseCase(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): String {
        return aiRepository.sendCardMessage(systemPrompt, history, userMessage)
    }
}
