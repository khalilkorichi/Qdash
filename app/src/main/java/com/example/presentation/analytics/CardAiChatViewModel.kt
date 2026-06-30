package com.example.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AiChatMessage
import com.example.domain.model.CardAiContext
import com.example.domain.model.ChatSender
import com.example.domain.usecase.ai.CardDbSnapshotUseCase
import com.example.domain.usecase.ai.SendCardAiMessageUseCase
import com.example.domain.ai.CardSystemPromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardAiChatState(
    val isSheetOpen: Boolean = false,
    val messages: List<AiChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isContextReady: Boolean = false,
    val tooltipContent: String = "",
    val cardTitle: String = ""
)

class CardAiChatViewModel(
    private val cardDbSnapshotUseCase: CardDbSnapshotUseCase,
    private val sendCardAiMessageUseCase: SendCardAiMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardAiChatState())
    val uiState: StateFlow<CardAiChatState> = _uiState.asStateFlow()

    private var currentContext: CardAiContext? = null
    private var systemPrompt: String = ""

    fun openSheet(cardContext: CardAiContext) {
        currentContext = cardContext
        _uiState.update {
            it.copy(
                isSheetOpen = true,
                cardTitle = cardContext.cardTitle,
                tooltipContent = cardContext.tooltipContent,
                messages = emptyList(),
                inputText = "",
                isContextReady = false
            )
        }
        
        viewModelScope.launch {
            try {
                val snapshot = cardDbSnapshotUseCase(
                    periodStart = cardContext.periodStart,
                    periodEnd = cardContext.periodEnd,
                    cardType = cardContext.cardType
                )
                systemPrompt = CardSystemPromptBuilder.build(cardContext, snapshot)
                _uiState.update { it.copy(isContextReady = true) }
            } catch (e: Exception) {
                systemPrompt = "أنت مستشار مالي لبطاقة ${cardContext.cardTitle}."
                _uiState.update { it.copy(isContextReady = true) }
            }
        }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false) }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return
        
        val userMessage = AiChatMessage(
            sender = ChatSender.USER,
            message = text,
            sessionTitle = currentContext?.cardId ?: "card_chat"
        )
        
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }
        
        viewModelScope.launch {
            try {
                val replyText = sendCardAiMessageUseCase(
                    systemPrompt = systemPrompt,
                    history = _uiState.value.messages.dropLast(1),
                    userMessage = text
                )
                
                val aiMessage = AiChatMessage(
                    sender = ChatSender.AI,
                    message = replyText,
                    sessionTitle = currentContext?.cardId ?: "card_chat"
                )
                
                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = AiChatMessage(
                    sender = ChatSender.AI,
                    message = "حدث خطأ أثناء معالجة رسالتك. يرجى المحاولة مرة أخرى.",
                    sessionTitle = currentContext?.cardId ?: "card_chat"
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isLoading = false
                    )
                }
            }
        }
    }
}
