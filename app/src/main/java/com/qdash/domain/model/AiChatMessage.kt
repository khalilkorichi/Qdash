package com.qdash.domain.model

import com.qdash.data.local.entities.AiChatMessageEntity
import com.qdash.domain.model.common.Identifiable

enum class ChatSender {
    USER, AI
}

data class AiChatMessage(
    override val /* contract */ id: Long = 0,
    val sender: ChatSender,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionTitle: String
) : Identifiable

fun AiChatMessageEntity.toDomain() = AiChatMessage(
    id = id,
    sender = ChatSender.valueOf(sender),
    message = message,
    timestamp = timestamp,
    sessionTitle = sessionTitle
)

fun AiChatMessage.toEntity() = AiChatMessageEntity(
    id = id,
    sender = sender.name,
    message = message,
    timestamp = timestamp,
    sessionTitle = sessionTitle
)
