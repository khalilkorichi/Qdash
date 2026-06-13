package com.example.presentation.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.ChatSender
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.repository.AiRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.CategoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AiModelInfo(
    val id: String,
    val name: String,
    val provider: String
)

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isMiniChatOpen: Boolean = false,
    val isLoading: Boolean = false,
    val inputText: String = "",
    val currentSessionTitle: String = "محادثة جديدة",
    val sessions: List<String> = emptyList(),
    val selectedModelId: String = "gemini-2.5-flash",
    val models: List<AiModelInfo> = listOf(
        AiModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", "Google"),
        AiModelInfo("gemini-3.1-flash", "Gemini 3.1 Flash", "Google"),
        AiModelInfo("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite", "Google"),
        AiModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", "Google"),
        AiModelInfo("gemini-3.1-pro", "Gemini 3.1 Pro", "Google"),
        AiModelInfo("gemini-3-flash-preview", "Gemini 3 Flash Preview", "Google"),
        AiModelInfo("glm-5.1", "glm-5.1", "Z.ai")
    )
)

class AiChatViewModel(
    private val aiRepository: AiRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var messagesCollectJob: Job? = null

    init {
        // Collect suggestions
        viewModelScope.launch {
            val initialSuggestions = aiRepository.getInitialSuggestions()
            _uiState.update { it.copy(suggestions = initialSuggestions) }
        }

        // Collect distinct session titles from DB to display in drawer
        viewModelScope.launch {
            aiRepository.getAllSessionTitles().collectLatest { sessionList ->
                _uiState.update { it.copy(sessions = sessionList) }
            }
        }

        // Initialize by loading default session messages
        loadSessionMessages(_uiState.value.currentSessionTitle)

        // Load saved model preference
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        val savedModelId = sharedPrefs.getString("selected_ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        _uiState.update { it.copy(selectedModelId = savedModelId) }
    }

    fun selectModel(modelId: String) {
        val sharedPrefs = context.getSharedPreferences("kdach_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selected_ai_model", modelId).apply()
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun selectSession(sessionTitle: String) {
        _uiState.update { it.copy(currentSessionTitle = sessionTitle) }
        loadSessionMessages(sessionTitle)
    }

    fun createNewSession() {
        _uiState.update { it.copy(currentSessionTitle = "محادثة جديدة") }
        loadSessionMessages("محادثة جديدة")
    }

    fun deleteSession(sessionTitle: String) {
        viewModelScope.launch {
            aiRepository.deleteSession(sessionTitle)
            if (_uiState.value.currentSessionTitle == sessionTitle) {
                createNewSession()
            }
        }
    }

    private fun loadSessionMessages(sessionTitle: String) {
        messagesCollectJob?.cancel()
        messagesCollectJob = viewModelScope.launch {
            if (sessionTitle == "محادثة جديدة") {
                _uiState.update { it.copy(messages = emptyList()) }
                return@launch
            }
            
            // Load accounts and categories once to parse drafts locally
            val accounts = accountRepository.getAllAccounts().first()
            val categories = categoryRepository.getAllCategories().first()

            aiRepository.getMessagesBySession(sessionTitle).collectLatest { dbMessages ->
                val uiMessages = dbMessages.map { msg ->
                    // Auto-parse draft transaction if the AI text corresponds to a transaction creation proposal
                    val draftTx = parseDraftTransactionFromText(msg.message, accounts, categories)
                    AiChatMessage(
                        id = msg.id.toString(),
                        text = msg.message,
                        isUser = msg.sender == ChatSender.USER,
                        timestamp = msg.timestamp,
                        draftTransaction = draftTx,
                        categoryName = categories.firstOrNull()?.name ?: "غير محدد",
                        accountName = accounts.firstOrNull()?.name ?: "غير محدد"
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }
    }

    private fun parseDraftTransactionFromText(text: String, accounts: List<Account>, categories: List<Category>): Transaction? {
        if (text.contains("مسودة") && (text.contains("بقيمة") || text.contains("سجل"))) {
            val amountRegex = """(?:\b|دج\s*)(\d+(?:\.\d+)?)\s*(?:دج)?""".toRegex()
            val amount = amountRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 150.0
            
            // Note extraction logic
            var extractedNote = "مسودة مسترجعة من محادثة AI"
            val noteKeywords = listOf("لشراء", "مقابل", "على", "بخصوص", "غرض")
            for (keyword in noteKeywords) {
                if (text.contains(keyword)) {
                    val index = text.indexOf(keyword) + keyword.length
                    val rest = text.substring(index).trim()
                    if (rest.isNotEmpty()) {
                        val endIdx = rest.indexOfAny(charArrayOf('.', '\n', ',', '،', '؛', '!'))
                        val rawNote = if (endIdx != -1) rest.substring(0, endIdx).trim() else rest
                        if (rawNote.isNotEmpty()) {
                            extractedNote = rawNote.take(40)
                            break
                        }
                    }
                }
            }

            // Transaction Type detection logic
            val type = if (text.contains("راتب") || text.contains("دخل") || text.contains("وارد") || text.contains("أودع")) {
                TransactionType.INCOME
            } else if (text.contains("تحويل") || text.contains("حول")) {
                TransactionType.TRANSFER
            } else {
                TransactionType.EXPENSE
            }

            return Transaction(
                amount = amount,
                type = type,
                categoryId = categories.firstOrNull { it.type.name == type.name }?.id ?: categories.firstOrNull()?.id ?: 1L,
                accountId = accounts.firstOrNull()?.id ?: 1L,
                note = extractedNote,
                date = System.currentTimeMillis()
            )
        }
        return null
    }

    fun toggleMiniChat() {
        _uiState.update { it.copy(isMiniChatOpen = !it.isMiniChatOpen) }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectSuggestion(suggestion: String) {
        _uiState.update { it.copy(inputText = suggestion) }
        sendMessage(suggestion)
    }

    fun sendMessage(text: String = _uiState.value.inputText) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        _uiState.update { it.copy(inputText = "", isLoading = true) }

        viewModelScope.launch {
            var sessionTitle = _uiState.value.currentSessionTitle
            
            // If starting a new chat session, rename session automatically from user prompt
            if (sessionTitle == "محادثة جديدة") {
                var cleanText = trimmedText.trim()
                // Remove trailing question marks or punctuation
                cleanText = cleanText.removeSuffix("؟").removeSuffix("?").removeSuffix(".").removeSuffix("!").trim()
                
                // Remove leading question particles in Arabic
                val questionParticles = listOf("هل يمكنني", "هل يمكن", "هل", "كيف يمكنني", "كيف يمكن", "كيف", "أين", "ما هي", "ما هو", "ما", "كم", "لماذا")
                for (particle in questionParticles) {
                    if (cleanText.startsWith(particle)) {
                        cleanText = cleanText.substring(particle.length).trim()
                        break
                    }
                }
                
                val words = cleanText.split(" ").filter { it.isNotBlank() }
                val baseTitle = if (words.size > 3) words.take(3).joinToString(" ") else cleanText
                sessionTitle = if (baseTitle.length > 25) baseTitle.take(22) + "..." else baseTitle
                if (sessionTitle.isBlank()) sessionTitle = "محادثة جديدة"
                _uiState.update { it.copy(currentSessionTitle = sessionTitle) }
                loadSessionMessages(sessionTitle)
            }

            try {
                // Generate and save to database
                aiRepository.generateAiResponse(sessionTitle, trimmedText, _uiState.value.selectedModelId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                // If failed, insert error message
                aiRepository.insertMessage(
                    com.example.domain.model.AiChatMessage(
                        sender = ChatSender.AI,
                        message = "عذراً، حدث خطأ أثناء الاتصال بالخادم: ${e.localizedMessage}",
                        sessionTitle = sessionTitle
                    )
                )
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun confirmDraft(messageId: String) {
        viewModelScope.launch {
            val targetMsg = _uiState.value.messages.find { it.id == messageId && it.draftTransaction != null }
            if (targetMsg != null && targetMsg.draftTransaction != null) {
                transactionRepository.insertTransaction(targetMsg.draftTransaction)
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(isConfirmed = true) else msg
                    })
                }
            }
        }
    }

    fun cancelDraft(messageId: String) {
        val messages = _uiState.value.messages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(isCancelled = true)
            } else {
                msg
            }
        }
        _uiState.update { it.copy(messages = messages) }
    }

    fun clearChat() {
        viewModelScope.launch {
            val title = _uiState.value.currentSessionTitle
            aiRepository.clearHistory(title)
            createNewSession()
        }
    }
}
