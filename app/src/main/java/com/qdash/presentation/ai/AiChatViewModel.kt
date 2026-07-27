package com.qdash.presentation.ai

import androidx.compose.runtime.Immutable

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.domain.model.ChatSender
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.TransferRequest
import com.qdash.domain.model.RecentActivitySummary
import com.qdash.domain.model.WalletDistributionSuggestion
import com.qdash.domain.model.LowBalanceAlertState
import com.qdash.domain.model.TransferDraftState
import com.qdash.domain.model.SelectedAccountDetailsState
import com.qdash.domain.model.QuickImpactPreviewState
import com.qdash.domain.model.AiVoiceState
import com.qdash.domain.repository.AiRepository
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.SavingRepository
import com.qdash.domain.usecase.ai.GetRecentActivitySummaryUseCase
import com.qdash.domain.usecase.ai.GetWalletDistributionUseCase
import com.qdash.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase
import com.qdash.domain.usecase.ai.GetQuickImpactPreviewUseCase
import com.qdash.domain.usecase.transfer.TransferBetweenAccountsUseCase
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

enum class AiModelAvailability {
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

enum class AiErrorType {
    NETWORK_ERROR,
    AI_ERROR
}

data class AiErrorState(
    val type: AiErrorType,
    val message: String
)

@Immutable
data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isMiniChatOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isAiTyping: Boolean = false,
    val inputText: String = "",
    val currentSessionTitle: String = "محادثة جديدة",
    val sessions: List<String> = emptyList(),
    val selectedModelId: String = "gemini-3.6-flash-high",
    val proactiveInsights: List<String> = emptyList(),
    val models: List<AiModelInfo> = listOf(
        AiModelInfo("gemini-3.6-flash-high", "Gemini Flash 3.6 (High)", "Google"),
        AiModelInfo("gemini-3.6-flash-medium", "Gemini Flash 3.6 (Medium)", "Google"),
        AiModelInfo("gemini-3.6-flash-low", "Gemini Flash 3.6 (Low)", "Google"),
        AiModelInfo("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", "Google"),
        AiModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", "Google"),
        AiModelInfo("gemini-3.1-flash", "Gemini 3.1 Flash", "Google"),
        AiModelInfo("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite", "Google"),
        AiModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", "Google"),
        AiModelInfo("gemini-3.1-pro", "Gemini 3.1 Pro", "Google"),
        AiModelInfo("gemini-3-flash-preview", "Gemini 3 Flash Preview", "Google"),
        AiModelInfo("glm-5.1", "glm-5.1", "Z.ai"),
        // OpenRouter Models
        AiModelInfo("deepseek/deepseek-r1:free", "DeepSeek R1", "DeepSeek"),
        AiModelInfo("cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "Dolphin Mistral 24B Venice", "Cognitive Computations"),
        AiModelInfo("openrouter/free", "Free Models Router", "OpenRouter"),
        AiModelInfo("google/gemma-2-9b-it:free", "Gemma 2 9B", "Google"),
        AiModelInfo("google/gemma-4-26b-a4b:free", "Gemma 4 26B A4B", "Google"),
        AiModelInfo("openai/gpt-oss-120b:free", "GPT OSS 120B", "OpenAI"),
        AiModelInfo("openai/gpt-oss-20b:free", "GPT OSS 20B", "OpenAI"),
        AiModelInfo("poolside/laguna-m-1:free", "Laguna M.1", "Poolside"),
        AiModelInfo("poolside/laguna-xs-2:free", "Laguna XS.2", "Poolside"),
        AiModelInfo("liquidai/lfm2.5-1.2b-thinking:free", "LFM2.5 1.2B Thinking", "Liquid AI"),
        AiModelInfo("meta-llama/llama-3.1-8b-instruct:free", "Llama 3.1 8B Instruct", "Meta"),
        AiModelInfo("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B Instruct", "Meta"),
        AiModelInfo("nvidia/llama-nemotron-embed-vl-1b-v2:free", "Llama Nemotron Embed VL", "NVIDIA"),
        AiModelInfo("mistralai/mistral-7b-instruct:free", "Mistral 7B Instruct", "Mistral AI"),
        AiModelInfo("nvidia/nemotron-3-nano-30b-a3b:free", "Nemotron 3 Nano", "NVIDIA"),
        AiModelInfo("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", "Nemotron 3 Nano Omni", "NVIDIA"),
        AiModelInfo("nvidia/nemotron-3-super-120b-a12b:free", "Nemotron 3 Super", "NVIDIA"),
        AiModelInfo("nvidia/nemotron-3-ultra-55b:free", "Nemotron 3 Ultra", "NVIDIA"),
        AiModelInfo("cohere/north-mini-code:free", "North Mini Code", "Cohere"),
        AiModelInfo("qwen/qwen-2.5-7b-instruct:free", "Qwen 2.5 7B Instruct", "Qwen"),
        AiModelInfo("qwen/qwen-3-coder-32b-instruct:free", "Qwen 3 Coder 32B", "Qwen"),
        AiModelInfo("stepfun/step-3.5-flash:free", "Step 3.5 Flash", "StepFun"),
        AiModelInfo("arcee/trinity-large-preview:free", "Trinity Large Preview", "Arcee AI"),
        AiModelInfo("arcee/trinity-mini:free", "Trinity Mini", "Arcee AI"),
        // OpenCode Models
        AiModelInfo("opencode/big-pickle", "Big Pickle (GLM-4.6)", "OpenCode"),
        AiModelInfo("opencode/deepseek-v4-flash-free", "DeepSeek V4 Flash", "OpenCode"),
        AiModelInfo("opencode/nemotron-3-super-free", "Nemotron 3 Super", "OpenCode")
    ),
    val modelAvailability: Map<String, AiModelAvailability> = emptyMap(),
    // Available accounts and categories — fed into the editable draft dropdowns
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    // Failure state
    val error: AiErrorState? = null,
    val lastFailedText: String? = null
)

class AiChatViewModel(
    private val aiRepository: AiRepository,
    internal val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    internal val preferencesManager: com.qdash.core.preferences.PreferencesManager,
    private val getRecentActivitySummaryUseCase: GetRecentActivitySummaryUseCase,
    private val getWalletDistributionUseCase: GetWalletDistributionUseCase,
    internal val evaluateLowBalanceAlertsUseCase: EvaluateLowBalanceAlertsUseCase,
    private val getQuickImpactPreviewUseCase: GetQuickImpactPreviewUseCase,
    internal val transferBetweenAccountsUseCase: TransferBetweenAccountsUseCase,
    private val savingRepository: SavingRepository
) : ViewModel() {

    internal val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val parser = AiChatReplyParser(
        transactionRepository = transactionRepository,
        savingRepository = savingRepository,
        getRecentActivitySummaryUseCase = getRecentActivitySummaryUseCase,
        getWalletDistributionUseCase = getWalletDistributionUseCase,
        evaluateLowBalanceAlertsUseCase = evaluateLowBalanceAlertsUseCase,
        getQuickImpactPreviewUseCase = getQuickImpactPreviewUseCase
    )

    private val _voiceState = MutableStateFlow<AiVoiceState>(AiVoiceState.Idle)
    val voiceState: StateFlow<AiVoiceState> = _voiceState.asStateFlow()

    private val _voiceText = MutableStateFlow("")
    val voiceText: StateFlow<String> = _voiceText.asStateFlow()

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

        // Collect accounts and categories for editable draft dropdowns
        viewModelScope.launch {
            accountRepository.getAllAccounts().collectLatest { accountList ->
                _uiState.update { it.copy(accounts = accountList) }
            }
        }
        viewModelScope.launch {
            categoryRepository.getAllCategories().collectLatest { categoryList ->
                _uiState.update { it.copy(categories = categoryList) }
            }
        }

        // Initialize by loading default session messages
        loadSessionMessages(_uiState.value.currentSessionTitle)

        // Load saved model preference
        val savedModelId = preferencesManager.selectedAiModel
        _uiState.update { it.copy(selectedModelId = savedModelId) }

        checkModelAvailability()

        // Generate proactive insights
        generateProactiveInsights()
    }

    fun selectModel(modelId: String) {
        preferencesManager.selectedAiModel = modelId
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun checkModelAvailability() {
        val models = _uiState.value.models
        _uiState.update {
            it.copy(modelAvailability = models.associate { model -> model.id to AiModelAvailability.CHECKING })
        }
        viewModelScope.launch {
            models.map { model ->
                launch {
                    val isAvailable = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                        runCatching {
                            aiRepository.generateResponse("اختبار توفر النموذج. أجب بكلمة OK فقط.", model.id)
                        }.getOrNull()?.replyText?.isNotBlank() == true
                    } == true

                    _uiState.update { state ->
                        state.copy(
                            modelAvailability = state.modelAvailability + (
                                model.id to if (isAvailable) AiModelAvailability.AVAILABLE else AiModelAvailability.UNAVAILABLE
                                )
                        )
                    }
                }
            }
        }
    }

    fun startVoiceListening() {
        _voiceState.value = AiVoiceState.Listening
        _voiceText.value = ""
    }

    fun updateVoicePartial(text: String) {
        _voiceText.value = text
        if (text.isNotBlank() && _voiceState.value is AiVoiceState.Idle) {
            _voiceState.value = AiVoiceState.Listening
        }
    }

    fun stopVoiceListening() {
        val text = _voiceText.value.trim()
        _voiceState.value = if (text.isBlank()) {
            AiVoiceState.Idle
        } else {
            AiVoiceState.Transcribed(text)
        }
    }

    fun setVoiceProcessing() {
        _voiceState.value = AiVoiceState.Processing
    }

    fun setVoiceError(message: String) {
        _voiceState.value = AiVoiceState.Error(message)
    }

    fun clearVoiceInput() {
        _voiceText.value = ""
        _voiceState.value = AiVoiceState.Idle
    }

    fun preFillMessage(text: String) {
        if (text.isNotBlank()) {
            setInputText(text)
        }
    }

    fun sendVoiceMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        clearVoiceInput()
        sendMessage(trimmed)
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
                    val isAiMessage = msg.sender == ChatSender.AI
                    val isTransferReply = isAiMessage && parser.isTransferDraftReply(msg.message)
                    val isDraftReply = isAiMessage && !isTransferReply && parser.isTransactionDraftReply(msg.message)
                    val isAccountDetailsReply = isAiMessage && !isTransferReply && !isDraftReply && parser.isSelectedAccountDetailsReply(msg.message, accounts)
                    val isRecentReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && parser.isRecentActivityReply(msg.message)
                    val isDistributionReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && parser.isWalletDistributionReply(msg.message)
                    val isLowBalanceReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && parser.isLowBalanceAlertReply(msg.message)
                    val isBalanceReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && !isRecentReply && !isDistributionReply && !isLowBalanceReply && parser.isGeneralBalanceReply(msg.message)

                    val draftTx = if (isDraftReply) {
                        parser.parseDraftTransactionFromText(msg.message, accounts, categories)
                    } else null

                    val walletSnapshot = if (isBalanceReply) {
                        parser.buildWalletSnapshot(accounts)
                    } else null

                    val recentActivity = if (isRecentReply) {
                        parser.getRecentActivitySummary()
                    } else null

                    val walletDistribution = if (isDistributionReply) {
                        parser.getWalletDistributionSuggestion()
                    } else null

                    val lowBalanceAlert = if (isLowBalanceReply) {
                        parser.evaluateLowBalanceAlerts()
                    } else null

                    val transferDraft = if (isTransferReply) {
                        parser.parseTransferDraftFromText(msg.message, accounts)
                    } else null

                    val selectedAccountDetails = if (isAccountDetailsReply) {
                        parser.buildSelectedAccountDetails(msg.message, accounts)
                    } else null

                    val quickImpactPreview = if (isAiMessage && draftTx != null) {
                        parser.getQuickImpactPreview(draftTx.amount, draftTx.type, draftTx.categoryId, draftTx.accountId)
                    } else null

                    AiChatMessage(
                        id = msg.id.toString(),
                        text = msg.message,
                        isUser = msg.sender == ChatSender.USER,
                        timestamp = msg.timestamp,
                        draftTransaction = draftTx,
                        categoryName = categories.find { it.id == draftTx?.categoryId }?.name ?: "غير محدد",
                        accountName = accounts.firstOrNull { it.id == draftTx?.accountId }?.name ?: "غير محدد",
                        walletSnapshot = walletSnapshot,
                        recentActivitySummary = recentActivity,
                        walletDistributionSuggestion = walletDistribution,
                        lowBalanceAlertState = lowBalanceAlert,
                        transferDraftState = transferDraft,
                        selectedAccountDetailsState = selectedAccountDetails,
                        quickImpactPreviewState = quickImpactPreview,
                        transferFromAccountName = accounts.find { it.id == (transferDraft?.fromAccountId ?: 0L) }?.name ?: "غير محدد",
                        transferToAccountName = accounts.find { it.id == (transferDraft?.toAccountId ?: 0L) }?.name ?: "غير محدد"
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }
    }



    internal fun generateProactiveInsights() {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val categories = categoryRepository.getAllCategories().first()
                val insights = AiInsightsHelper.computeInsights(transactions, categories)
                _uiState.update { it.copy(proactiveInsights = insights) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

        _uiState.update { it.copy(inputText = "", isLoading = true, isAiTyping = true, error = null, lastFailedText = null) }

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
                _uiState.update { it.copy(isLoading = false, isAiTyping = false) }
                generateProactiveInsights() // Refresh insights dynamically
            } catch (e: Exception) {
                val errorType = when (e) {
                    is com.qdash.domain.model.AiFailureException.NetworkFailure -> AiErrorType.NETWORK_ERROR
                    else -> AiErrorType.AI_ERROR
                }
                val friendlyMessage = when (errorType) {
                    AiErrorType.NETWORK_ERROR -> "فشل في الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت وإعادة المحاولة."
                    AiErrorType.AI_ERROR -> "حدث خطأ في استجابة المساعد الذكي: ${e.localizedMessage ?: "فشل في الاتصال بالخادم"}"
                }
                _uiState.update { it.copy(
                    isLoading = false,
                    isAiTyping = false,
                    error = AiErrorState(type = errorType, message = friendlyMessage),
                    lastFailedText = trimmedText
                ) }
            }
        }
    }


    fun clearChat() {
        viewModelScope.launch {
            val title = _uiState.value.currentSessionTitle
            aiRepository.clearHistory(title)
            createNewSession()
        }
    }

    fun retryLastMessage() {
        val lastText = _uiState.value.lastFailedText
        if (!lastText.isNullOrBlank()) {
            sendMessage(lastText)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, lastFailedText = null) }
    }
}
