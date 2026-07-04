package com.qdash.domain.model

sealed class AiVoiceState {
    data object Idle : AiVoiceState()
    data object Listening : AiVoiceState()
    data object Processing : AiVoiceState()
    data class Transcribed(val text: String) : AiVoiceState()
    data class Error(val message: String) : AiVoiceState()
}
