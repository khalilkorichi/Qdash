package com.qdash.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.AiChatMessage
import com.qdash.domain.model.CardAiContext
import com.qdash.domain.model.ChatSender
import com.qdash.domain.usecase.ai.CardDbSnapshotUseCase
import com.qdash.domain.repository.AiRepository
import com.qdash.domain.ai.CardSystemPromptBuilder
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
    val cardTitle: String = "",
    val thinkingStep: Int = 0 // 0: Loading DB data, 1: Analyzing patterns, 2: Formulating recommendations, 3: Done/Idle
)

class CardAiChatViewModel(
    private val cardDbSnapshotUseCase: CardDbSnapshotUseCase,
    private val aiRepository: AiRepository
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
                isContextReady = false,
                thinkingStep = 0
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
            } catch (e: Exception) {
                systemPrompt = "أنت مستشار مالي لبطاقة ${cardContext.cardTitle}."
            }
            _uiState.update { it.copy(isContextReady = true, thinkingStep = 1) }
            triggerAutoAnalysis()
        }
    }

    private fun triggerAutoAnalysis() {
        if (_uiState.value.isLoading) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        val stepJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _uiState.update { 
                if (it.thinkingStep == 1) it.copy(thinkingStep = 2) else it
            }
        }
        
        viewModelScope.launch {
            try {
                val initialPrompt = "حلل هذه البيانات المالية للبطاقة الحالية واقترح توصيات عملية لترشيد الاستهلاك وتحسين الادخار."
                val replyText = aiRepository.sendCardMessage(
                    systemPrompt = systemPrompt,
                    history = emptyList(),
                    userMessage = initialPrompt
                )
                
                val aiMessage = AiChatMessage(
                    sender = ChatSender.AI,
                    message = replyText,
                    sessionTitle = currentContext?.cardId ?: "card_chat"
                )
                
                stepJob.cancel()
                _uiState.update {
                    it.copy(
                        messages = listOf(aiMessage),
                        isLoading = false,
                        thinkingStep = 3
                    )
                }
            } catch (e: Exception) {
                stepJob.cancel()
                val errorMsgText = if (e.message?.contains("All AI providers failed") == true) {
                    "تعذّر الاتصال بالمساعد الذكي. تحقق من اتصالك بالإنترنت."
                } else {
                    "حدث خطأ أثناء تحليل البيانات. يرجى المحاولة مرة أخرى."
                }
                val errorMessage = AiChatMessage(
                    sender = ChatSender.AI,
                    message = errorMsgText,
                    sessionTitle = currentContext?.cardId ?: "card_chat"
                )
                _uiState.update {
                    it.copy(
                        messages = listOf(errorMessage),
                        isLoading = false,
                        thinkingStep = 3
                    )
                }
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
                val replyText = aiRepository.sendCardMessage(
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
                val errorMsgText = if (e.message?.contains("All AI providers failed") == true) {
                    "تعذّر الاتصال بالمساعد الذكي. تحقق من اتصالك بالإنترنت."
                } else {
                    "حدث خطأ أثناء معالجة رسالتك. يرجى المحاولة مرة أخرى."
                }
                val errorMessage = AiChatMessage(
                    sender = ChatSender.AI,
                    message = errorMsgText,
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
