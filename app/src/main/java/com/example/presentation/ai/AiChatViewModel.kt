package com.example.presentation.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.ChatSender
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.TransferRequest
import com.example.domain.model.RecentActivitySummary
import com.example.domain.model.WalletDistributionSuggestion
import com.example.domain.model.LowBalanceAlertState
import com.example.domain.model.TransferDraftState
import com.example.domain.model.SelectedAccountDetailsState
import com.example.domain.model.QuickImpactPreviewState
import com.example.domain.model.AiVoiceState
import com.example.domain.repository.AiRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.SavingRepository
import com.example.domain.usecase.ai.GetRecentActivitySummaryUseCase
import com.example.domain.usecase.ai.GetWalletDistributionUseCase
import com.example.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase
import com.example.domain.usecase.ai.GetQuickImpactPreviewUseCase
import com.example.domain.usecase.transfer.TransferBetweenAccountsUseCase
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

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isMiniChatOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isAiTyping: Boolean = false,
    val inputText: String = "",
    val currentSessionTitle: String = "محادثة جديدة",
    val sessions: List<String> = emptyList(),
    val selectedModelId: String = "gemini-2.5-flash",
    val proactiveInsights: List<String> = emptyList(),
    val models: List<AiModelInfo> = listOf(
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
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: com.example.core.preferences.PreferencesManager,
    private val getRecentActivitySummaryUseCase: GetRecentActivitySummaryUseCase,
    private val getWalletDistributionUseCase: GetWalletDistributionUseCase,
    private val evaluateLowBalanceAlertsUseCase: EvaluateLowBalanceAlertsUseCase,
    private val getQuickImpactPreviewUseCase: GetQuickImpactPreviewUseCase,
    private val transferBetweenAccountsUseCase: TransferBetweenAccountsUseCase,
    private val savingRepository: SavingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

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

    /**
     * Returns true if the AI message text is a general wallet/portfolio balance reply
     * (not a specific-account balance query).
     */
    private fun isGeneralBalanceReply(text: String): Boolean {
        val lower = text.lowercase()
        val generalMarkers = listOf(
            "إجمالي رصيد", "رصيد المحفظة", "رصيدك الإجمالي", "مجموع أرصدة", "إجمالي أرصدة",
            "رصيد", "الرصيد", "أرصدة", "الأرصدة", "رصيدي", "أموالي", "فلوسي", "ميزانيتي", "الميزانية", "المحفظة",
            "إجمالي الدخل", "إجمالي المصاريف",
            "balance", "balances", "portfolio", "wallet", "total balance", "cash", "money", "dzd"
        )
        return generalMarkers.any { lower.contains(it) }
    }

    private fun isTransactionDraftReply(text: String): Boolean {
        val lower = text.lowercase()
        val draftMarkers = listOf(
            "مسودة معاملة", "معاملة مقترحة", "تأكيد المعاملة", "هل تريد تأكيد", "سأقوم بتسجيل",
            "تم فهم المعاملة", "سجلت لك", "اقتراح تسجيل", "مسودة مصروف", "مسودة دخل", "معاملة جديدة",
            "هل تأكد", "أرغب في تسجيل", "سأسجل", "قم بتسجيل", "معاملة بـ", "مبلغ المعاملة",
            "transaction draft", "proposed transaction", "confirm transaction", "record transaction"
        )
        val actionMarkers = listOf("شراء", "شريت", "صرفت", "دخل", "راتب", "أودعت", "مصروف", "مصاريف",
            "سجل", "سأقوم", "دفع", "دفعت", "اشتريت", "خصمت", "أضفت", "حصلت على", "استلمت", "ربحت")
        return draftMarkers.any { lower.contains(it) } ||
            (actionMarkers.any { lower.contains(it) } && parseDarijaAmount(text) != null)
    }

    private fun isRecentActivityReply(text: String): Boolean {
        val lower = text.lowercase()
        val activityMarkers = listOf(
            "آخر المعاملات", "آخر معاملات", "آخر حركة", "آخر الحركات", "النشاط الأخير",
            "المعاملات الأخيرة", "سجل المعاملات", "تاريخ المعاملات", "الحركات الأخيرة",
            "آخر 10", "آخر 5", "أخيرة", "المعاملات السابقة", "سجل الإنفاق",
            "recent activity", "last transactions", "recent transactions", "transaction history",
            "last 10", "last 5", "previous transactions"
        )
        return activityMarkers.any { lower.contains(it) }
    }

    private fun isWalletDistributionReply(text: String): Boolean {
        val lower = text.lowercase()
        val distributionMarkers = listOf(
            "توزيع المحفظة", "توزيع أموالك", "توزيع الحسابات", "نسبة توزيع", "كيف تتوزع", "توزيع أرصدتك",
            "نسبة كل حساب", "توزيع مدخراتك", "توزيع ثروتك", "نسبة الأموال", "حصة كل حساب",
            "wallet distribution", "portfolio distribution", "distribution of funds", "fund allocation"
        )
        return distributionMarkers.any { lower.contains(it) }
    }

    private fun isLowBalanceAlertReply(text: String): Boolean {
        val lower = text.lowercase()
        val alertMarkers = listOf(
            "رصيد منخفض", "الأرصدة المنخفضة", "تنبيه رصيد", "حد الرصيد", "تنبيه الرصيد",
            "رصيد ضعيف", "تحذير رصيد", "الحساب منخفض", "رصيد قليل", "رصيد صغير",
            "حد أدنى", "تحت الحد", "تجاوز الحد", "يحذر", "خطر الرصيد",
            "low balance", "balance alert", "low balance alert", "minimum balance", "balance warning"
        )
        return alertMarkers.any { lower.contains(it) }
    }

    private fun isTransferDraftReply(text: String): Boolean {
        val lower = text.lowercase()
        val transferMarkers = listOf(
            "مسودة تحويل", "تحويل مقترح", "تأكيد التحويل", "حول من", "تحويل من", "نقل من",
            "نقل أموال", "تحويل مبلغ", "سأقوم بتحويل", "تحويل داخلي", "نقل داخلي",
            "تحويل إلى", "تحويل بين الحسابات", "تحويل الرصيد",
            "transfer draft", "proposed transfer", "confirm transfer", "internal transfer"
        )
        return transferMarkers.any { lower.contains(it) }
    }

    private fun isSelectedAccountDetailsReply(text: String, accounts: List<Account>): Boolean {
        val lower = text.lowercase()
        val detailsMarkers = listOf(
            "تفاصيل الحساب", "معلومات الحساب", "كشف الحساب", "account details",
            "رصيد حساب", "رصيد الحساب", "تفاصيل حساب", "حساب الـ", "حساب ال",
            "معلومات عن حساب", "بيانات الحساب", "إحصائيات الحساب"
        )
        val nameMatch = accounts.any {
            val accName = it.name.lowercase()
            lower.contains(accName) ||
            (it.type.name == "CCP" && (lower.contains("ccp") || lower.contains("بريدي"))) ||
            (it.type.name == "CASH" && (lower.contains("كاش") || lower.contains("نقدي") || lower.contains("نقد"))) ||
            (it.type.name == "BARIDIMOB" && (lower.contains("بريدي موب") || lower.contains("baridimob"))) ||
            (it.type.name == "BANK" && lower.contains("بنك"))
        }
        return (detailsMarkers.any { lower.contains(it) } || lower.contains("رصيد")) && nameMatch
    }

    private fun isQuickImpactPreviewReply(text: String): Boolean {
        val lower = text.lowercase()
        val impactMarkers = listOf(
            "التأثير المالي", "تأثير سريع", "تأثير على ميزانيتك", "تأثير على رصيدك",
            "التأثير على", "quick impact", "financial impact", "budget impact",
            "ماذا سيحدث", "بعد المعاملة", "بعد الإضافة", "سيتغير رصيدك",
            "نتيجة المعاملة", "تداعيات مالية"
        )
        return impactMarkers.any { lower.contains(it) }
    }

    /**
     * Builds a WalletSnapshot from current accounts for embedding in an AI balance message.
     */
    private fun buildWalletSnapshot(accounts: List<Account>): WalletSnapshot {
        val totalBalance = accounts.filter { !it.isArchived }.sumOf { it.balance }
        val items = accounts.filter { !it.isArchived }.map { acc ->
            val typeLabel = when (acc.type) {
                com.example.domain.model.AccountType.BANK -> "بنك"
                com.example.domain.model.AccountType.CCP -> "CCP"
                com.example.domain.model.AccountType.BARIDIMOB -> "بريدي موب"
                com.example.domain.model.AccountType.CASH -> "نقداً"
                com.example.domain.model.AccountType.SAVINGS -> "ادخار"
                com.example.domain.model.AccountType.WALLET -> "محفظة"
                com.example.domain.model.AccountType.OTHER -> "أخرى"
            }
            AccountBalanceItem(
                id = acc.id,
                name = acc.name,
                typeLabel = typeLabel,
                balance = acc.balance,
                currency = acc.currency,
                color = acc.color
            )
        }
        return WalletSnapshot(totalBalance = totalBalance, currency = "دج", accounts = items)
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
                    val isTransferReply = isAiMessage && isTransferDraftReply(msg.message)
                    val isDraftReply = isAiMessage && !isTransferReply && isTransactionDraftReply(msg.message)
                    val isAccountDetailsReply = isAiMessage && !isTransferReply && !isDraftReply && isSelectedAccountDetailsReply(msg.message, accounts)
                    val isRecentReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && isRecentActivityReply(msg.message)
                    val isDistributionReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && isWalletDistributionReply(msg.message)
                    val isLowBalanceReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && isLowBalanceAlertReply(msg.message)
                    val isBalanceReply = isAiMessage && !isTransferReply && !isDraftReply && !isAccountDetailsReply && !isRecentReply && !isDistributionReply && !isLowBalanceReply && isGeneralBalanceReply(msg.message)

                    val draftTx = if (isDraftReply) {
                        parseDraftTransactionFromText(msg.message, accounts, categories)
                    } else null

                    val walletSnapshot = if (isBalanceReply) {
                        buildWalletSnapshot(accounts)
                    } else null

                    val recentActivity = if (isRecentReply) {
                        getRecentActivitySummaryUseCase()
                    } else null

                    val walletDistribution = if (isDistributionReply) {
                        getWalletDistributionUseCase()
                    } else null

                    val lowBalanceAlert = if (isLowBalanceReply) {
                        evaluateLowBalanceAlertsUseCase()
                    } else null

                    val transferDraft = if (isTransferReply) {
                        parseTransferDraftFromText(msg.message, accounts)
                    } else null

                    val selectedAccountDetails = if (isAccountDetailsReply) {
                        buildSelectedAccountDetails(msg.message, accounts)
                    } else null

                    val quickImpactPreview = if (isAiMessage && draftTx != null) {
                        getQuickImpactPreviewUseCase(draftTx.amount, draftTx.type, draftTx.categoryId, draftTx.accountId)
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

    private suspend fun parseTransferDraftFromText(text: String, accounts: List<Account>): TransferDraftState? {
        val lowerText = text.lowercase()
        val isTransfer = lowerText.contains("تحويل") || lowerText.contains("حول")
        if (isTransfer) {
            val amount = parseDarijaAmount(text) ?: 500.0
            
            // Find from account and to account in text
            var fromAcc = accounts.firstOrNull { lowerText.contains(it.name.lowercase()) }
            var toAcc = accounts.filter { it.id != fromAcc?.id }.firstOrNull { lowerText.contains(it.name.lowercase()) }
            
            if (fromAcc == null) {
                fromAcc = accounts.firstOrNull()
            }
            if (toAcc == null) {
                toAcc = accounts.firstOrNull { it.id != fromAcc?.id } ?: accounts.firstOrNull()
            }
            
            val note = "تحويل مسجل عن طريق المساعد الذكي"
            
            return TransferDraftState(
                amount = amount,
                fromAccountId = fromAcc?.id ?: 1L,
                toAccountId = toAcc?.id ?: 2L,
                note = note,
                fromAccountName = fromAcc?.name ?: "غير محدد",
                toAccountName = toAcc?.name ?: "غير محدد"
            )
        }
        return null
    }

    private suspend fun buildSelectedAccountDetails(text: String, accounts: List<Account>): SelectedAccountDetailsState? {
        val matchedAccount = accounts.firstOrNull { acc ->
            text.lowercase().contains(acc.name.lowercase())
        } ?: accounts.firstOrNull() ?: return null
        
        val recentTxs = transactionRepository.getTransactionsByAccount(matchedAccount.id).first().take(3)
        val goals = savingRepository.getAllSavingGoals().first().filter { it.accountId == matchedAccount.id }
        
        return SelectedAccountDetailsState(
            account = matchedAccount,
            recentTransactions = recentTxs,
            activeGoals = goals
        )
    }

    private fun parseDarijaAmount(text: String): Double? {
        val clean = text.replace("،", "").replace(",", "").trim()
        
        // Check for "مليون" or "ملاين"
        val millionRegex = """(\d+)\s*(?:مليون|ملاين|ملايين)""".toRegex()
        val millionMatch = millionRegex.find(clean)
        if (millionMatch != null) {
            return millionMatch.groupValues[1].toDouble() * 10000.0
        }
        if (clean.contains("زوج ملاين") || clean.contains("زوج ملايين") || clean.contains("2 ملاين")) {
            return 20000.0
        }
        if (clean.contains("مليون") && !clean.contains("مليونين")) {
            return 10000.0
        }

        // Check for "ألف" or "الاف" (meaning centimes in pricing, so X * 10 DZD)
        val thousandRegex = """(\d+)\s*(?:الف|ألف|الاف|آلاف)""".toRegex()
        val thousandMatch = thousandRegex.find(clean)
        if (thousandMatch != null) {
            return thousandMatch.groupValues[1].toDouble() * 10.0
        }
        
        // Words representation of thousands
        if (clean.contains("عشرة الاف") || clean.contains("عشرة آلاف") || clean.contains("عشرتلاف") || clean.contains("عشرتالاف")) {
            return 100.0
        }
        if (clean.contains("عشرين الف") || clean.contains("عشرين ألف")) {
            return 200.0
        }
        if (clean.contains("خمسين الف") || clean.contains("خمسين ألف")) {
            return 500.0
        }
        if (clean.contains("مية الف") || clean.contains("مية ألف") || clean.contains("مائة ألف")) {
            return 1000.0
        }

        // Check for "فرنك" (X / 100 DZD)
        val francRegex = """(\d+)\s*(?:فرنك|فرانك)""".toRegex()
        val francMatch = francRegex.find(clean)
        if (francMatch != null) {
            return francMatch.groupValues[1].toDouble() / 100.0
        }

        // Check for "دورو" (X * 0.05 DZD)
        val doroRegex = """(\d+)\s*(?:دورو)""".toRegex()
        val doroMatch = doroRegex.find(clean)
        if (doroMatch != null) {
            return doroMatch.groupValues[1].toDouble() * 0.05
        }

        // Check for standard "دج" or "دينار" or "DA"
        val dzdRegex = """(\d+(?:\.\d+)?)\s*(?:دج|دينار|da|dzd)""".toRegex(RegexOption.IGNORE_CASE)
        val dzdMatch = dzdRegex.find(clean)
        if (dzdMatch != null) {
            return dzdMatch.groupValues[1].toDouble()
        }

        // Standard fallback number
        val genericRegex = """\b(\d+(?:\.\d+)?)\b""".toRegex()
        val genericMatch = genericRegex.find(clean)
        if (genericMatch != null) {
            return genericMatch.groupValues[1].toDouble()
        }

        return null
    }

    private fun findCategoryByKeywords(text: String, categories: List<Category>, type: TransactionType): Category? {
        val clean = text.lowercase()
        val transportKeywords = listOf("مازوت", "توموبيل", "طوموبيل", "ترونسبور", "كار", "طاكسي", "مواصلات", "بنزين", "ايسونس", "سيارة")
        val foodKeywords = listOf("حليب", "خبز", "قضيان", "ماكلة", "مطعم", "قهوة", "شاي", "عشاء", "غداء", "فطور", "سوبرماركت", "خضار", "فواكه")
        val billsKeywords = listOf("تريسيتي", "ماء", "غاز", "فليكسي", "انترنت", "كونيكسيو", "كارط", "فاتورة", "كهرباء", "هاتف")
        val shoppingKeywords = listOf("شريت", "حوايج", "لبسة", "سباط", "مشتريات", "تيكنولوجيا", "تليفون")
        
        val targetKeywordGroup = when {
            transportKeywords.any { clean.contains(it) } -> transportKeywords
            foodKeywords.any { clean.contains(it) } -> foodKeywords
            billsKeywords.any { clean.contains(it) } -> billsKeywords
            shoppingKeywords.any { clean.contains(it) } -> shoppingKeywords
            else -> emptyList()
        }
        
        if (targetKeywordGroup.isNotEmpty()) {
            val matched = categories.firstOrNull { cat ->
                cat.type.name == type.name && (
                    targetKeywordGroup.any { keyword -> cat.name.contains(keyword) || keyword.contains(cat.name) }
                )
            }
            if (matched != null) return matched
        }
        
        return categories.firstOrNull { it.type.name == type.name } ?: categories.firstOrNull()
    }

    private fun parseDraftTransactionFromText(text: String, accounts: List<Account>, categories: List<Category>): Transaction? {
        val lowerText = text.lowercase()
        val containsKeywords = lowerText.contains("مسودة") || 
                lowerText.contains("سجل") || 
                lowerText.contains("شريت") || 
                lowerText.contains("صرفت") || 
                lowerText.contains("دخلت") || 
                lowerText.contains("أودعت") || 
                lowerText.contains("حول") || 
                lowerText.contains("فرنك") || 
                lowerText.contains("دج")
                
        if (containsKeywords) {
            val amount = parseDarijaAmount(text) ?: return null
            
            // Note extraction logic
            var extractedNote = "عملية مسجلة بالصوت/الأمر الذكي"
            val noteKeywords = listOf("شريت", "سجل", "سجلي", "صرفت", "لشراء", "مقابل", "على", "بخصوص", "غرض", "شراء")
            for (keyword in noteKeywords) {
                if (text.contains(keyword)) {
                    val index = text.indexOf(keyword) + keyword.length
                    val rest = text.substring(index).trim()
                    if (rest.isNotEmpty()) {
                        val endIdx = rest.indexOfAny(charArrayOf('.', '\n', ',', '،', '؛', '!', 'ب', 'd', 'D', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'))
                        val rawNote = if (endIdx != -1) rest.substring(0, endIdx).trim() else rest
                        if (rawNote.isNotEmpty()) {
                            extractedNote = rawNote.take(40)
                            break
                        }
                    }
                }
            }
            if (extractedNote.length < 2) {
                extractedNote = "عملية مالية"
            }

            // Transaction Type detection logic
            val type = if (lowerText.contains("راتب") || lowerText.contains("دخل") || lowerText.contains("وارد") || lowerText.contains("أودع") || lowerText.contains("خلصت")) {
                TransactionType.INCOME
            } else if (lowerText.contains("تحويل") || lowerText.contains("حول")) {
                TransactionType.TRANSFER
            } else {
                TransactionType.EXPENSE
            }

            val category = findCategoryByKeywords(text, categories, type)

            return Transaction(
                amount = amount,
                type = type,
                categoryId = category?.id ?: 1L,
                accountId = accounts.firstOrNull()?.id ?: 1L,
                note = extractedNote,
                date = System.currentTimeMillis()
            )
        }
        return null
    }

    private fun generateProactiveInsights() {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val categories = categoryRepository.getAllCategories().first()
                val insights = mutableListOf<String>()

                val now = System.currentTimeMillis()
                val oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000L
                val twoWeeksAgo = now - 14 * 24 * 60 * 60 * 1000L

                val thisWeekTxs = transactions.filter { it.date in oneWeekAgo..now && it.type == TransactionType.EXPENSE }
                val lastWeekTxs = transactions.filter { it.date in twoWeeksAgo..oneWeekAgo && it.type == TransactionType.EXPENSE }

                val thisWeekTotal = thisWeekTxs.sumOf { it.amount }
                val lastWeekTotal = lastWeekTxs.sumOf { it.amount }

                if (lastWeekTotal > 0.0) {
                    val percentChange = ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100.0
                    if (percentChange > 10.0) {
                        insights.add("⚠️ لاحظت زيادة بنسبة %.1f%% في إجمالي مصاريفك هذا الأسبوع مقارنة بالأسبوع الماضي.".format(percentChange))
                    } else if (percentChange < -10.0) {
                        insights.add("🎉 ممتاز! انخفضت مصاريفك بنسبة %.1f%% هذا الأسبوع مقارنة بالأسبوع الماضي.".format(-percentChange))
                    }
                }

                // Spending by category this week
                val categoryGroups = thisWeekTxs.groupBy { it.categoryId }
                val topCategoryEntry = categoryGroups.maxByOrNull { it.value.sumOf { tx -> tx.amount } }
                if (topCategoryEntry != null) {
                    val catName = categories.find { it.id == topCategoryEntry.key }?.name ?: "أخرى"
                    val catTotal = topCategoryEntry.value.sumOf { it.amount }
                    insights.add("📊 فئة \"$catName\" هي الأعلى إنفاقاً هذا الأسبوع بإجمالي %s.".format(com.example.core.utils.FormatterUtils.formatCurrency(catTotal)))
                }

                // Balance alert or cash warnings
                val totalSpentThisWeek = thisWeekTotal
                if (totalSpentThisWeek > 50000.0) {
                    insights.add("💡 لقد أنفقت أكثر من 50,000 دج في الـ 7 أيام الأخيرة. قد ترغب في مراجعة ميزانيتك.")
                }

                // Fallback / standard financial tips if empty
                if (insights.isEmpty()) {
                    insights.add("💡 تلميحة: تقسيم راتبك بنسبة 50/30/20 (الاحتياجات، الرغبات، الادخار) هو البداية الصحيحة للحرية المالية.")
                    insights.add("💡 حافظ على تدوين كل المصاريف اليومية الصغيرة لتكشف أين تذهب أموالك بدقة.")
                }

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
                    is com.example.domain.model.AiFailureException.NetworkFailure -> AiErrorType.NETWORK_ERROR
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

    fun confirmDraft(messageId: String) {
        viewModelScope.launch {
            val targetMsg = _uiState.value.messages.find { it.id == messageId && it.draftTransaction != null }
            if (targetMsg != null && targetMsg.draftTransaction != null) {
                // Merge edited fields (user overrides) with the original AI-parsed draft
                val baseDraft = targetMsg.draftTransaction
                val finalTransaction = baseDraft.copy(
                    amount = targetMsg.editedAmount ?: baseDraft.amount,
                    type = targetMsg.editedType ?: baseDraft.type,
                    note = targetMsg.editedNote ?: baseDraft.note,
                    categoryId = targetMsg.editedCategoryId ?: baseDraft.categoryId,
                    accountId = targetMsg.editedAccountId ?: baseDraft.accountId
                )
                transactionRepository.insertTransaction(finalTransaction)
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(isConfirmed = true) else msg
                    })
                }
                generateProactiveInsights() // Update insights on new confirmed transaction
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

    fun updateDraftField(messageId: String, field: DraftField, value: Any) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id != messageId) return@map msg
                when (field) {
                    DraftField.AMOUNT -> msg.copy(editedAmount = (value as? Double))
                    DraftField.TYPE -> msg.copy(editedType = (value as? TransactionType))
                    DraftField.NOTE -> msg.copy(editedNote = (value as? String))
                    DraftField.CATEGORY_ID -> {
                        val catId = (value as? Long)
                        val catName = _uiState.value.categories.find { it.id == catId }?.name
                        msg.copy(editedCategoryId = catId, categoryName = catName ?: msg.categoryName)
                    }
                    DraftField.ACCOUNT_ID -> {
                        val accId = (value as? Long)
                        val accName = _uiState.value.accounts.find { it.id == accId }?.name
                        msg.copy(editedAccountId = accId, accountName = accName ?: msg.accountName)
                    }
                    DraftField.TRANSFER_AMOUNT -> msg.copy(editedTransferAmount = (value as? Double))
                    DraftField.TRANSFER_FROM_ACCOUNT_ID -> {
                        val accId = (value as? Long)
                        val name = _uiState.value.accounts.find { it.id == accId }?.name
                        msg.copy(editedTransferFromAccountId = accId, transferFromAccountName = name ?: msg.transferFromAccountName)
                    }
                    DraftField.TRANSFER_TO_ACCOUNT_ID -> {
                        val accId = (value as? Long)
                        val name = _uiState.value.accounts.find { it.id == accId }?.name
                        msg.copy(editedTransferToAccountId = accId, transferToAccountName = name ?: msg.transferToAccountName)
                    }
                    DraftField.TRANSFER_NOTE -> msg.copy(editedTransferNote = (value as? String))
                    DraftField.LOW_BALANCE_LIMIT -> msg.copy(editedLowBalanceLimit = (value as? Double))
                }
            })
        }
    }

    fun confirmTransfer(messageId: String) {
        viewModelScope.launch {
            val targetMsg = _uiState.value.messages.find { it.id == messageId && it.transferDraftState != null }
            if (targetMsg != null && targetMsg.transferDraftState != null) {
                val draft = targetMsg.transferDraftState
                val finalAmount = targetMsg.editedTransferAmount ?: draft.amount
                val finalFrom = targetMsg.editedTransferFromAccountId ?: draft.fromAccountId
                val finalTo = targetMsg.editedTransferToAccountId ?: draft.toAccountId
                val finalNote = targetMsg.editedTransferNote ?: draft.note
                
                val req = TransferRequest(
                    fromAccountId = finalFrom,
                    toAccountId = finalTo,
                    amount = finalAmount,
                    note = finalNote,
                    date = System.currentTimeMillis()
                )
                val success = transferBetweenAccountsUseCase(req)
                if (success) {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(isTransferConfirmed = true) else msg
                        })
                    }
                    generateProactiveInsights()
                }
            }
        }
    }

    fun cancelTransfer(messageId: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == messageId) msg.copy(isTransferCancelled = true) else msg
            })
        }
    }

    fun saveLowBalanceLimit(messageId: String, limit: Double) {
        viewModelScope.launch {
            preferencesManager.lowBalanceLimit = limit
            val newState = evaluateLowBalanceAlertsUseCase(limit)
            _uiState.update { state ->
                state.copy(messages = state.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            lowBalanceAlertState = newState,
                            editedLowBalanceLimit = null
                        )
                    } else msg
                })
            }
        }
    }

    fun updateLowBalanceLimitField(messageId: String, limit: Double) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(editedLowBalanceLimit = limit)
                } else msg
            })
        }
    }

    fun duplicateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val copy = transaction.copy(id = 0, date = System.currentTimeMillis())
            transactionRepository.insertTransaction(copy)
            generateProactiveInsights()
        }
    }

    fun startEditingTransaction(messageId: String, transaction: Transaction) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        draftTransaction = transaction,
                        editedAmount = transaction.amount,
                        editedType = transaction.type,
                        editedNote = transaction.note,
                        editedCategoryId = transaction.categoryId,
                        editedAccountId = transaction.accountId,
                        isConfirmed = false,
                        isCancelled = false
                    )
                } else msg
            })
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
