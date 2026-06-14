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
    val proactiveInsights: List<String> = emptyList(),
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

        // Generate proactive insights
        generateProactiveInsights()
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
                        categoryName = categories.find { it.id == draftTx?.categoryId }?.name ?: "غير محدد",
                        accountName = accounts.firstOrNull()?.name ?: "غير محدد"
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }
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
                    insights.add("📊 فئة \"$catName\" هي الأعلى إنفاقاً هذا الأسبوع بإجمالي %s دج.".format(com.example.core.utils.FormatterUtils.formatCurrency(catTotal)))
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
                generateProactiveInsights() // Refresh insights dynamically
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

    fun clearChat() {
        viewModelScope.launch {
            val title = _uiState.value.currentSessionTitle
            aiRepository.clearHistory(title)
            createNewSession()
        }
    }
}
