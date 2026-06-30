package com.example.domain.usecase.ai

import com.example.domain.model.AiChatMessage
import com.example.domain.repository.AiRepository

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
