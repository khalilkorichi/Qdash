package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_chat_messages",
    indices = [Index(value = ["sessionTitle"])]
)
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "AI"
    val message: String,
    val timestamp: Long,
    val sessionTitle: String
)
