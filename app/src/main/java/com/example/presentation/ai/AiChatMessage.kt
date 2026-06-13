package com.example.presentation.ai

import com.example.domain.model.Transaction
import java.util.UUID

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val draftTransaction: Transaction? = null,
    val categoryName: String? = null,
    val accountName: String? = null,
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false
)
