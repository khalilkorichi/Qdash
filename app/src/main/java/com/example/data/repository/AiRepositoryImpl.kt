package com.example.data.repository

import com.example.data.local.dao.AiChatDao
import com.example.domain.model.AiChatMessage
import com.example.domain.model.ChatSender
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
import com.example.domain.model.AiFailureException
import com.example.domain.repository.*
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiRepositoryImpl(
    private val aiChatDao: AiChatDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetGoalRepository: BudgetGoalRepository,
    private val debtRepository: DebtRepository,
    private val notificationRepository: NotificationRepository,
    private val geminiApiKey: String,
    private val openRouterApiKey: String,
    private val nvidiaApiKey: String,
    private val isTesting: Boolean = false
) : AiRepository {

    private val resolvedGeminiApiKey by lazy { if (geminiApiKey.isNotBlank()) geminiApiKey else (getApiKey() ?: "") }
    private val resolvedOpenRouterApiKey by lazy {
        if (openRouterApiKey.isNotBlank()) openRouterApiKey else {
            try {
                com.example.core.utils.CryptoUtils.decrypt("Tsi1wycL1+Cbdm+wfFhc7DpgC1ksFwwFolpzWWCA5KxiS3sH+NziI+JQQKe3+dYOZdAJkKa6aDwRm6NArnknrYyT6RmJWxwNhkf3A3eiWiVvuhZ1YSM8aNAuLaGo9/TQy1J7mbA=")
            } catch (e: Exception) {
                ""
            }
        }
    }
    private val resolvedNvidiaApiKey by lazy { nvidiaApiKey }

    private val providers: List<com.example.data.ai.AiProvider> by lazy {
        listOf(
            com.example.data.ai.providers.GeminiProvider(resolvedGeminiApiKey, okHttpClient),
            com.example.data.ai.providers.OpenRouterProvider(resolvedOpenRouterApiKey, okHttpClient),
            com.example.data.ai.providers.NvidiaProvider(resolvedNvidiaApiKey, okHttpClient)
        )
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                val addresses = okhttp3.Dns.SYSTEM.lookup(hostname)
                val ipv4 = addresses.filter { it is java.net.Inet4Address }
                return if (ipv4.isNotEmpty()) ipv4 else addresses
            }
        })
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Retrieve API key from system property, environment variable, or BuildConfig if exists
    private fun getApiKey(): String? {
        val key = System.getProperty("GEMINI_API_KEY") 
            ?: System.getenv("GEMINI_API_KEY")
            ?: try {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.getField("GEMINI_API_KEY")
                field.get(null) as? String
            } catch (e: Exception) {
                null
            }
        if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") {
            return key
        }
        return try {
            com.example.core.utils.CryptoUtils.decrypt("BGkbMuKghm13XtkofUZqwvC7DV6mfwhpbglo9IQI5TJPat5FxTvzpmyHWIEqB6IXmV/wnDr7AYzhJqZlt5/L4hx2XJekQJXgcKlxyZf8DJV9")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private val AGENT_ROUTER_API_KEY = System.getenv("AGENT_ROUTER_API_KEY") ?: ""
    private val AGENT_ROUTER_BASE_URL = "https://agentrouter.org/v1"

    private val OPENROUTER_API_KEY by lazy {
        try {
            com.example.core.utils.CryptoUtils.decrypt("Tsi1wycL1+Cbdm+wfFhc7DpgC1ksFwwFolpzWWCA5KxiS3sH+NziI+JQQKe3+dYOZdAJkKa6aDwRm6NArnknrYyT6RmJWxwNhkf3A3eiWiVvuhZ1YSM8aNAuLaGo9/TQy1J7mbA=")
        } catch (e: Exception) {
            ""
        }
    }
    private val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

    private val OPENCODE_API_KEY by lazy {
        try {
            com.example.core.utils.CryptoUtils.decrypt("15bkTg0mKwU6Rn0XKnXXM+JrxD3Y/Yc8106zO7lGWlFu0quR2wqv0Sg9WzALUff0DpeAKQxbcVDGhlW+JU74X4NiYV4DQ5xq9yq0xs2ILwlVKaoB396+PXcKfTODCaY=")
        } catch (e: Exception) {
            ""
        }
    }
    private val OPENCODE_BASE_URL = "https://opencode.ai/zen/v1"

    override suspend fun generateResponse(prompt: String, modelId: String): AiResponse {
        val normalized = prompt.trim().lowercase()
        // Check if the user is asking to add or record a transaction to create a draft transaction.
        if (normalized.contains("add") || normalized.contains("insert") || normalized.contains("إضافة") || normalized.contains("سجل")) {
            val amountRegex = "\\d+".toRegex()
            val match = amountRegex.find(normalized)
            val amount = match?.value?.toDoubleOrNull() ?: 150.0

            val accounts = accountRepository.getAllAccounts().first()
            val categories = categoryRepository.getAllCategories().first()

            val account = accounts.firstOrNull()
            val category = categories.firstOrNull()

            val draft = Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = category?.id ?: 1L,
                accountId = account?.id ?: 1L,
                note = "AI generated transaction from: $prompt",
                date = System.currentTimeMillis()
            )

            return AiResponse(
                replyText = "لقد قمت بإنشاء مسودة لمعاملة بقيمة **$amount دج**. هل تريد تأكيد تسجيلها؟",
                draftTransaction = draft,
                categoryName = category?.name ?: "غير محدد",
                accountName = account?.name ?: "غير محدد"
            )
        }

        // Otherwise, run our standard helper with fallback routing
        var lastException: Exception? = null
        val modelsToTry = listOf(modelId) + (modelFallbackMap[modelId] ?: emptyList())
        
        for (candidateModelId in modelsToTry) {
            try {
                val textReply = tryGenerateResponse("Default", prompt, candidateModelId)
                return AiResponse(replyText = textReply)
            } catch (e: Exception) {
                lastException = e
                try {
                    android.util.Log.w("AiRepository", "Failed to get response from model $candidateModelId, trying fallback. Error: ${e.localizedMessage}")
                } catch (logEx: Exception) {
                    println("AiRepository: Failed to get response from model $candidateModelId, trying fallback. Error: ${e.localizedMessage}")
                }
            }
        }
        
        throw lastException ?: AiFailureException.AiServiceFailure("فشل الاتصال بالمساعد الذكي للنموذج $modelId")
    }

    override suspend fun getInitialSuggestions(): List<String> {
        return listOf(
            "عرض الحسابات 💳",
            "أضف مصروف 500 دج بقالة 🛒",
            "كم صرفت اليوم؟ 💸",
            "عرض الميزانية 📊"
        )
    }

    override fun getMessagesBySession(sessionTitle: String): Flow<List<AiChatMessage>> {
        return aiChatDao.getMessagesBySession(sessionTitle).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertMessage(message: AiChatMessage): Long {
        return aiChatDao.insertMessage(message.toEntity())
    }

    override suspend fun clearHistory(sessionTitle: String) {
        aiChatDao.clearHistory(sessionTitle)
    }

    override suspend fun generateAiResponse(sessionTitle: String, userPrompt: String, modelId: String): String {
        // Prevent inserting duplicate user prompt when retrying a failed attempt
        val history = getMessagesBySession(sessionTitle).first()
        val isLastAlreadyThisPrompt = history.isNotEmpty() && 
                history.last().sender == ChatSender.USER && 
                history.last().message.trim() == userPrompt.trim()

        if (!isLastAlreadyThisPrompt) {
            insertMessage(
                AiChatMessage(
                    sender = ChatSender.USER,
                    message = userPrompt,
                    sessionTitle = sessionTitle
                )
            )
        }

        var lastException: Exception? = null
        val modelsToTry = listOf(modelId) + (modelFallbackMap[modelId] ?: emptyList())
        var aiResponse = ""
        var success = false
        
        for (candidateModelId in modelsToTry) {
            try {
                aiResponse = tryGenerateResponse(sessionTitle, userPrompt, candidateModelId)
                success = true
                break
            } catch (e: Exception) {
                lastException = e
                try {
                    android.util.Log.w("AiRepository", "Failed to get response from model $candidateModelId, trying fallback. Error: ${e.localizedMessage}")
                } catch (logEx: Exception) {
                    println("AiRepository: Failed to get response from model $candidateModelId, trying fallback. Error: ${e.localizedMessage}")
                }
            }
        }
        
        if (!success) {
            throw lastException ?: AiFailureException.AiServiceFailure("فشل الاتصال بالمساعد الذكي للنموذج $modelId")
        }

        // Split response by [NEXT] and insert each as a separate sequential message
        val parts = aiResponse.split("[NEXT]")
        var tempTime = System.currentTimeMillis()
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isNotEmpty()) {
                insertMessage(
                    AiChatMessage(
                        sender = ChatSender.AI,
                        message = trimmed,
                        sessionTitle = sessionTitle,
                        timestamp = tempTime
                    )
                )
                tempTime += 50 // small millisecond offset for correct ordering
            }
        }

        return aiResponse
    }

    private suspend fun callAgentRouterApi(
        sessionTitle: String,
        userPrompt: String,
        modelId: String,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey else AGENT_ROUTER_API_KEY
            val url = "$AGENT_ROUTER_BASE_URL/chat/completions"
            val history = getMessagesBySession(sessionTitle).first().takeLast(10)
            val filteredHistory = if (history.isNotEmpty() && 
                history.last().sender == ChatSender.USER && 
                history.last().message.trim() == userPrompt.trim()) {
                history.dropLast(1)
            } else {
                history
            }
            
            val dbContext = getDatabaseContextString()
            val messagesArray = JSONArray()
            // Do not add system instruction for glm-5.1 as it triggers safety filters (sensitive words detected)
            if (modelId != "glm-5.1") {
                messagesArray.put(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", """
                            أنت مساعد مالي ذكي ومحترف جداً لتطبيق قداشّ (Kdach) لإدارة المصاريف والميزانيات في الجزائر.
                            قواعد السلوك والعمل:
                            1. تفاعل مع المستخدم بلغة عربية فصحى، مهذبة، وواضحة جداً.
                            2. عند طلب المستخدم تسجيل معاملة مادية (مصروف، دخل، أو تحويل)، قم أولاً بالبحث عن الفئات الحالية باستخدام أداة `get_categories` والحسابات الحالية باستخدام أداة `get_accounts`.
                            3. طابق المعاملة مع أقرب فئة موجودة مسبقاً في التطبيق دائماً كأولوية قصوى لتجنب تكرار وتضخم الفئات بشكل غير مبرر (مثال: إذا كان النشاط 'بقالة' أو 'أكل' طابقه مع فئة 'المواد الغذائية' أو 'الطعام' المتوفرة).
                            4. لا تقم بإنشاء فئة جديدة باستخدام أداة `create_category` إلا إذا كان النشاط مختلفاً تماماً عن كل الفئات المتوفرة ولا يمكن إدراجه تحت أي منها إطلاقاً.
                            5. تصرف بمسؤولية واحترافية عالية، وقدم نصائح مالية مفيدة ومختصرة عند الحاجة.
                            6. قم بتقسيم إجاباتك المعقدة أو التحليلات التي تحتوي على أفكار متعددة أو خطوات عمل منفصلة إلى عدة فقرات/خطوات وافصل بين كل جزء والذي يليه بالرمز `[NEXT]`.
                            7. عندما يسأل المستخدم عن "الرصيد" أو "كم رصيدي" بشكل عام (دون تحديد حساب معيّن مثل CCP أو البنك)، يجب أن يكون الرد الافتراضي هو عرض إجمالي رصيد المحفظة (مجموع أرصدة كل الحسابات) أولاً كإجابة ذكية ومباشرة، مع إضافة اقتراح واضح وذكي في نهاية الرد يقترح إمكانية الاستعلام عن كل حساب على حدة، بصياغة مثل: "يمكنني أيضاً عرض تفاصيل كل حساب على حدة عند الطلب".
                            8. إذا طلب المستخدم رصيد حساب معيّن أو فئة حساب معيّنة (مثل: "رصيد CCP" أو "حساب البنك" أو "النقدي/كاش" أو "محفظتي/Wallet")، يجب عرض تفاصيل رصيد هذا الحساب المحدد فقط دون البقية.
                            
                            $dbContext
                        """.trimIndent())
                    }
                )
            }
            
            for (msg in filteredHistory) {
                val role = if (msg.sender == ChatSender.USER) "user" else "assistant"
                messagesArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", msg.message)
                    }
                )
            }
            // Add current prompt
            messagesArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                }
            )

            val requestBodyJson = JSONObject().apply {
                put("model", modelId)
                put("messages", messagesArray)
                put("tools", buildOpenAiToolsJson())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $keyToUse")
                .addHeader("Content-Type", "application/json")
                .addHeader("Originator", "codex_cli_rs")
                .addHeader("User-Agent", "codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464")
                .addHeader("Version", "0.101.0")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: java.io.IOException) {
                throw AiFailureException.NetworkFailure("فشل في الاتصال بخادم الذكاء الاصطناعي: ${e.localizedMessage}", e)
            }
            if (!response.isSuccessful) {
                throw AiFailureException.AiServiceFailure("فشل استجابة خادم الذكاء الاصطناعي برمز: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw AiFailureException.AiServiceFailure("تلقيت استجابة فارغة من خادم الذكاء الاصطناعي.")
            val responseJson = JSONObject(responseBody)
            val choices = responseJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw AiFailureException.AiServiceFailure("لم يتم إنشاء استجابة بواسطة الذكاء الاصطناعي.")
            }

            val choiceObj = choices.getJSONObject(0)
            val messageObj = choiceObj.optJSONObject("message") ?: throw AiFailureException.AiServiceFailure("تلقيت ردًا فارغًا من الخادم.")
            
            if (messageObj.has("tool_calls")) {
                val toolCalls = messageObj.getJSONArray("tool_calls")
                if (toolCalls.length() > 0) {
                    val toolCall = toolCalls.getJSONObject(0)
                    val functionObj = toolCall.getJSONObject("function")
                    val functionName = functionObj.getString("name")
                    val argumentsStr = functionObj.getString("arguments")
                    val argumentsJson = JSONObject(argumentsStr)
                    val toolCallId = toolCall.getString("id")

                    // Execute the tool call locally
                    val toolResultJson = executeToolCall(functionName, argumentsJson)

                    // Build the follow up messages array:
                    val followUpMessagesArray = JSONArray()
                    for (i in 0 until messagesArray.length()) {
                        followUpMessagesArray.put(messagesArray.getJSONObject(i))
                    }
                    followUpMessagesArray.put(messageObj)
                    followUpMessagesArray.put(JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", toolCallId)
                        put("name", functionName)
                        put("content", toolResultJson.toString())
                    })

                    val followUpRequestBodyJson = JSONObject().apply {
                        put("model", modelId)
                        put("messages", followUpMessagesArray)
                    }

                    val followUpRequest = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $keyToUse")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Originator", "codex_cli_rs")
                        .addHeader("User-Agent", "codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464")
                        .addHeader("Version", "0.101.0")
                        .post(followUpRequestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val followUpResponse = try {
                        okHttpClient.newCall(followUpRequest).execute()
                    } catch (e: java.io.IOException) {
                        throw AiFailureException.NetworkFailure("فشل في الاتصال بالخادم أثناء معالجة الأداة: ${e.localizedMessage}", e)
                    }
                    if (!followUpResponse.isSuccessful) {
                        throw AiFailureException.AiServiceFailure("فشل الخادم أثناء معالجة الأداة برمز الخطأ: ${followUpResponse.code} ${followUpResponse.message}")
                    }

                    val followUpResponseBody = followUpResponse.body?.string() ?: ""
                    val followUpResponseJson = JSONObject(followUpResponseBody)
                    val finalChoices = followUpResponseJson.optJSONArray("choices")
                    if (finalChoices == null || finalChoices.length() == 0) {
                        throw AiFailureException.AiServiceFailure("فشل المساعد في معالجة نتيجة الأداة.")
                    }
                    val finalMessage = finalChoices.getJSONObject(0).optJSONObject("message") ?: throw AiFailureException.AiServiceFailure("رد فارغ بعد استدعاء الأداة.")
                    return@withContext finalMessage.optString("content", "لم يتم إرجاع أي نص.")
                }
            }

            messageObj.optString("content", "لم يتم إرجاع أي نص.")
        } catch (e: Exception) {
            if (e is AiFailureException) throw e
            throw AiFailureException.AiServiceFailure("فشل الاتصال بالمساعد الذكي: ${e.localizedMessage}", e)
        }
    }

    private fun isOpenRouterModel(modelId: String): Boolean {
        return modelId.contains("/") && !modelId.startsWith("opencode/")
    }

    private fun isOpenCodeModel(modelId: String): Boolean {
        return modelId.startsWith("opencode/")
    }

    private fun modelSupportsTools(modelId: String): Boolean {
        if (modelId.startsWith("gemini-")) return true
        if (modelId == "glm-5.1") return true
        if (modelId.contains("llama-3.3-70b", ignoreCase = true)) return true
        if (modelId.contains("qwen-3-coder", ignoreCase = true)) return true
        return false
    }

    private val modelFallbackMap = mapOf(
        "gemini-2.5-flash" to listOf("google/gemini-2.5-flash:free", "google/gemini-2.5-flash"),
        "gemini-3.1-flash" to listOf("google/gemini-2.5-flash:free", "google/gemini-2.5-flash"),
        "gemini-2.5-flash-lite" to listOf("google/gemini-2.5-flash-lite"),
        "gemini-2.5-pro" to listOf("google/gemini-2.5-pro"),
        "gemini-3.1-pro" to listOf("google/gemini-3.1-pro"),
        "gemini-3-flash-preview" to listOf("google/gemini-2.5-flash:free"),
        "nvidia/nemotron-3-super-120b-a12b:free" to listOf("opencode/nemotron-3-super-free"),
        "opencode/nemotron-3-super-free" to listOf("nvidia/nemotron-3-super-120b-a12b:free")
    )

    private suspend fun tryGenerateResponse(sessionTitle: String, prompt: String, modelId: String): String {
        if (isTesting) {
            return simulateMockAiWithTools(prompt)
        }
        val apiKey = if (modelId.startsWith("gemini-")) getApiKey() else null
        return if (modelId.startsWith("gemini-")) {
            if (!apiKey.isNullOrBlank()) {
                callGeminiApiWithTools(apiKey, sessionTitle, prompt, modelId)
            } else {
                if (sessionTitle != "Default" && (prompt.contains("حلل") || prompt.contains("تقرير") || prompt.contains("تحليل"))) {
                    "الخطوة 1: جاري تحليل مصاريفك الإجمالية لشهر مايو... [NEXT] الخطوة 2: يظهر أن فئة المواد الغذائية تستهلك 45% من دخلك. [NEXT] الخطوة 3: ننصحك بتقليل الطلبات الخارجية لزيادة معدل ادخارك بنسبة 10%."
                } else {
                    val fallbacks = modelFallbackMap[modelId]
                    if (!fallbacks.isNullOrEmpty()) {
                        throw AiFailureException.AiServiceFailure("Google API key is missing, trying fallback.")
                    }
                    simulateMockAiWithTools(prompt)
                }
            }
        } else if (isOpenRouterModel(modelId)) {
            callOpenRouterApi(sessionTitle, prompt, modelId)
        } else if (isOpenCodeModel(modelId)) {
            callOpenCodeApi(sessionTitle, prompt, modelId)
        } else {
            callAgentRouterApi(sessionTitle, prompt, modelId, if (!apiKey.isNullOrBlank()) apiKey else null)
        }
    }

    private suspend fun callOpenRouterApi(
        sessionTitle: String,
        userPrompt: String,
        modelId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "$OPENROUTER_BASE_URL/chat/completions"
            val history = getMessagesBySession(sessionTitle).first().takeLast(10)
            val filteredHistory = if (history.isNotEmpty() && 
                history.last().sender == ChatSender.USER && 
                history.last().message.trim() == userPrompt.trim()) {
                history.dropLast(1)
            } else {
                history
            }
            
            val dbContext = getDatabaseContextString()
            val messagesArray = JSONArray()
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", """
                        أنت مساعد مالي ذكي ومحترف جداً لتطبيق قداشّ (Kdach) لإدارة المصاريف والميزانيات في الجزائر.
                        قواعد السلوك والعمل:
                        1. تفاعل مع المستخدم بلغة عربية فصحى، مهذبة، وواضحة جداً.
                        2. عند طلب المستخدم تسجيل معاملة مادية (مصروف، دخل، أو تحويل)، قم أولاً بالبحث عن الفئات الحالية باستخدام أداة `get_categories` والحسابات الحالية باستخدام أداة `get_accounts`.
                        3. طابق المعاملة مع أقرب فئة موجودة مسبقاً في التطبيق دائماً كأولوية قصوى لتجنب تكرار وتضخم الفئات بشكل غير مبرر.
                        4. لا تقم بإنشاء فئة جديدة باستخدام أداة `create_category` إلا إذا كان النشاط مختلفاً تماماً عن كل الفئات المتوفرة ولا يمكن إدراجه تحت أي منها إطلاقاً.
                        5. تصرف بمسؤولية واحترافية عالية، وقدم نصائح مالية مفيدة ومختصرة عند الحاجة.
                        6. قم بتقسيم إجاباتك المعقدة أو التحليلات التي تحتوي على أفكار متعددة أو خطوات عمل منفصلة إلى عدة فقرات/خطوات وافصل بين كل جزء والذي يليه بالرمز `[NEXT]`.
                        7. عندما يسأل المستخدم عن "الرصيد" أو "كم رصيدي" بشكل عام، يجب أن يكون الرد الافتراضي هو عرض إجمالي رصيد المحفظة أولاً كإجابة ذكية ومباشرة.
                        8. إذا طلب المستخدم رصيد حساب معيّن، يجب عرض تفاصيل رصيد هذا الحساب المحدد فقط دون البقية.
                        
                        $dbContext
                    """.trimIndent())
                }
            )
            
            for (msg in filteredHistory) {
                val role = if (msg.sender == ChatSender.USER) "user" else "assistant"
                messagesArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", msg.message)
                    }
                )
            }
            
            messagesArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                }
            )

            val requestBodyJson = JSONObject().apply {
                put("model", modelId)
                put("messages", messagesArray)
                if (modelSupportsTools(modelId) && !userPrompt.startsWith("اختبار توفر")) {
                    put("tools", buildOpenAiToolsJson())
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $OPENROUTER_API_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/khalilkorichi/Qdash")
                .addHeader("X-Title", "Qdash")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: java.io.IOException) {
                throw AiFailureException.NetworkFailure("فشل في الاتصال بخادم OpenRouter: ${e.localizedMessage}", e)
            }
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw AiFailureException.AiServiceFailure("فشل استجابة خادم OpenRouter برمز: ${response.code} ${response.message}. التفاصيل: $errorBody")
            }

            val responseBody = response.body?.string() ?: throw AiFailureException.AiServiceFailure("تلقيت استجابة فارغة من خادم OpenRouter.")
            val responseJson = JSONObject(responseBody)
            val choices = responseJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw AiFailureException.AiServiceFailure("لم يتم إنشاء استجابة بواسطة OpenRouter.")
            }

            val choiceObj = choices.getJSONObject(0)
            val messageObj = choiceObj.optJSONObject("message") ?: throw AiFailureException.AiServiceFailure("تلقيت ردًا فارغًا من الخادم.")
            
            if (messageObj.has("tool_calls")) {
                val toolCalls = messageObj.getJSONArray("tool_calls")
                if (toolCalls.length() > 0) {
                    val toolCall = toolCalls.getJSONObject(0)
                    val functionObj = toolCall.getJSONObject("function")
                    val functionName = functionObj.getString("name")
                    val argumentsStr = functionObj.getString("arguments")
                    val argumentsJson = JSONObject(argumentsStr)
                    val toolCallId = toolCall.getString("id")

                    val toolResultJson = executeToolCall(functionName, argumentsJson)

                    val followUpMessagesArray = JSONArray()
                    for (i in 0 until messagesArray.length()) {
                        followUpMessagesArray.put(messagesArray.getJSONObject(i))
                    }
                    followUpMessagesArray.put(messageObj)
                    followUpMessagesArray.put(JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", toolCallId)
                        put("name", functionName)
                        put("content", toolResultJson.toString())
                    })

                    val followUpRequestBodyJson = JSONObject().apply {
                        put("model", modelId)
                        put("messages", followUpMessagesArray)
                    }

                    val followUpRequest = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $OPENROUTER_API_KEY")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("HTTP-Referer", "https://github.com/khalilkorichi/Qdash")
                        .addHeader("X-Title", "Qdash")
                        .post(followUpRequestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val followUpResponse = try {
                        okHttpClient.newCall(followUpRequest).execute()
                    } catch (e: java.io.IOException) {
                        throw AiFailureException.NetworkFailure("فشل في الاتصال بالخادم أثناء معالجة الأداة: ${e.localizedMessage}", e)
                    }
                    if (!followUpResponse.isSuccessful) {
                        throw AiFailureException.AiServiceFailure("فشل خادم OpenRouter أثناء معالجة الأداة برمز الخطأ: ${followUpResponse.code} ${followUpResponse.message}")
                    }

                    val followUpResponseBody = followUpResponse.body?.string() ?: ""
                    val followUpResponseJson = JSONObject(followUpResponseBody)
                    val finalChoices = followUpResponseJson.optJSONArray("choices")
                    if (finalChoices == null || finalChoices.length() == 0) {
                        throw AiFailureException.AiServiceFailure("فشل المساعد في معالجة نتيجة الأداة.")
                    }
                    val finalMessage = finalChoices.getJSONObject(0).optJSONObject("message") ?: throw AiFailureException.AiServiceFailure("رد فارغ بعد استدعاء الأداة.")
                    return@withContext finalMessage.optString("content", "لم يتم إرجاع أي نص.")
                }
            }

            messageObj.optString("content", "لم يتم إرجاع أي نص.")
        } catch (e: Exception) {
            if (e is AiFailureException) throw e
            throw AiFailureException.AiServiceFailure("فشل الاتصال بـ OpenRouter: ${e.localizedMessage}", e)
        }
    }

    private suspend fun callOpenCodeApi(
        sessionTitle: String,
        userPrompt: String,
        modelId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "$OPENCODE_BASE_URL/chat/completions"
            val history = getMessagesBySession(sessionTitle).first().takeLast(10)
            val filteredHistory = if (history.isNotEmpty() && 
                history.last().sender == ChatSender.USER && 
                history.last().message.trim() == userPrompt.trim()) {
                history.dropLast(1)
            } else {
                history
            }
            
            val dbContext = getDatabaseContextString()
            val messagesArray = JSONArray()
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", """
                        أنت مساعد مالي ذكي ومحترف جداً لتطبيق قداشّ (Kdach) لإدارة المصاريف والميزانيات في الجزائر.
                        قواعد السلوك والعمل:
                        1. تفاعل مع المستخدم بلغة عربية فصحى، مهذبة، وواضحة جداً.
                        2. عند طلب المستخدم تسجيل معاملة مادية (مصروف، دخل، أو تحويل)، قم أولاً بالبحث عن الفئات الحالية باستخدام أداة `get_categories` والحسابات الحالية باستخدام أداة `get_accounts`.
                        3. طابق المعاملة مع أقرب فئة موجودة مسبقاً في التطبيق دائماً كأولوية قصوى لتجنب تكرار وتضخم الفئات بشكل غير مبرر.
                        4. لا تقم بإنشاء فئة جديدة باستخدام أداة `create_category` إلا إذا كان النشاط مختلفاً تماماً عن كل الفئات المتوفرة ولا يمكن إدراجه تحت أي منها إطلاقاً.
                        5. تصرف بمسؤولية واحترافية عالية، وقدم نصائح مالية مفيدة ومختصرة عند الحاجة.
                        6. قم بتقسيم إجاباتك المعقدة أو التحليلات التي تحتوي على أفكار متعددة أو خطوات عمل منفصلة إلى عدة فقرات/خطوات وافصل بين كل جزء والذي يليه بالرمز `[NEXT]`.
                        7. عندما يسأل المستخدم عن "الرصيد" أو "كم رصيدي" بشكل عام، يجب أن يكون الرد الافتراضي هو عرض إجمالي رصيد المحفظة أولاً كإجابة ذكية ومباشرة.
                        8. إذا طلب المستخدم رصيد حساب معيّن، يجب عرض تفاصيل رصيد هذا الحساب المحدد فقط دون البقية.
                        
                        $dbContext
                    """.trimIndent())
                }
            )
            
            for (msg in filteredHistory) {
                val role = if (msg.sender == ChatSender.USER) "user" else "assistant"
                messagesArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", msg.message)
                    }
                )
            }
            
            messagesArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                }
            )

            val cleanModelId = modelId.removePrefix("opencode/")
            val finalModelId = if (cleanModelId == "nemotron-3-super-free") "nemotron-3-ultra-free" else cleanModelId

            val requestBodyJson = JSONObject().apply {
                put("model", finalModelId)
                put("messages", messagesArray)
                if (modelSupportsTools(modelId) && !userPrompt.startsWith("اختبار توفر")) {
                    put("tools", buildOpenAiToolsJson())
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $OPENCODE_API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: java.io.IOException) {
                throw AiFailureException.NetworkFailure("فشل في الاتصال بخادم OpenCode: ${e.localizedMessage}", e)
            }
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw AiFailureException.AiServiceFailure("فشل استجابة خادم OpenCode برمز: ${response.code} ${response.message}. التفاصيل: $errorBody")
            }

            val responseBody = response.body?.string() ?: throw AiFailureException.AiServiceFailure("تلقيت استجابة فارغة من خادم OpenCode.")
            val responseJson = JSONObject(responseBody)
            val choices = responseJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw AiFailureException.AiServiceFailure("لم يتم إنشاء استجابة بواسطة OpenCode.")
            }

            val choiceObj = choices.getJSONObject(0)
            val messageObj = choiceObj.optJSONObject("message") ?: throw AiFailureException.AiServiceFailure("تلقيت ردًا فارغًا من الخادم.")
            
            if (messageObj.has("tool_calls")) {
                val toolCalls = messageObj.getJSONArray("tool_calls")
                if (toolCalls.length() > 0) {
                    val toolCall = toolCalls.getJSONObject(0)
                    val functionObj = toolCall.getJSONObject("function")
                    val functionName = functionObj.getString("name")
                    val argumentsStr = functionObj.getString("arguments")
                    val argumentsJson = JSONObject(argumentsStr)
                    val toolCallId = toolCall.getString("id")

                    val toolResultJson = executeToolCall(functionName, argumentsJson)

                    val followUpMessagesArray = JSONArray()
                    for (i in 0 until messagesArray.length()) {
                        followUpMessagesArray.put(messagesArray.getJSONObject(i))
                    }
                    followUpMessagesArray.put(messageObj)
                    followUpMessagesArray.put(JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", toolCallId)
                        put("name", functionName)
                        put("content", toolResultJson.toString())
                    })

                    val followUpRequestBodyJson = JSONObject().apply {
                        put("model", modelId)
                        put("messages", followUpMessagesArray)
                    }

                    val followUpRequest = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $OPENCODE_API_KEY")
                        .addHeader("Content-Type", "application/json")
                        .post(followUpRequestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val followUpResponse = try {
                        okHttpClient.newCall(followUpRequest).execute()
                    } catch (e: java.io.IOException) {
                        throw AiFailureException.NetworkFailure("فشل في الاتصال بالخادم أثناء معالجة الأداة: ${e.localizedMessage}", e)
                    }
                    if (!followUpResponse.isSuccessful) {
                        throw AiFailureException.AiServiceFailure("فشل خادم OpenCode أثناء معالجة الأداة برمز الخطأ: ${followUpResponse.code} ${followUpResponse.message}")
                    }

                    val followUpResponseBody = followUpResponse.body?.string() ?: ""
                    val followUpResponseJson = JSONObject(followUpResponseBody)
                    val finalChoices = followUpResponseJson.optJSONArray("choices")
                    if (finalChoices == null || finalChoices.length() == 0) {
                        throw AiFailureException.AiServiceFailure("فشل المساعد في معالجة نتيجة الأداة.")
                    }
                    val finalMessage = finalChoices.getJSONObject(0).optJSONObject("message") ?: throw AiFailureException.AiServiceFailure("رد فارغ بعد استدعاء الأداة.")
                    return@withContext finalMessage.optString("content", "لم يتم إرجاع أي نص.")
                }
            }

            messageObj.optString("content", "لم يتم إرجاع أي نص.")
        } catch (e: Exception) {
            if (e is AiFailureException) throw e
            throw AiFailureException.AiServiceFailure("فشل الاتصال بـ OpenCode: ${e.localizedMessage}", e)
        }
    }

    private suspend fun callGeminiApiWithTools(apiKey: String, sessionTitle: String, userPrompt: String, modelId: String): String = withContext(Dispatchers.IO) {
        try {
            val apiModelId = when (modelId) {
                "gemini-3.1-flash" -> "gemini-3.5-flash"
                "gemini-3.1-pro" -> "gemini-3.1-pro-preview"
                else -> modelId
            }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$apiModelId:generateContent?key=$apiKey"
            
            // Get previous chat history to pass as context
            val history = getMessagesBySession(sessionTitle).first().takeLast(10)
            val filteredHistory = if (history.isNotEmpty() && 
                history.last().sender == ChatSender.USER && 
                history.last().message.trim() == userPrompt.trim()) {
                history.dropLast(1)
            } else {
                history
            }
            
            val contentsArray = JSONArray()
            for (msg in filteredHistory) {
                val role = if (msg.sender == ChatSender.USER) "user" else "model"
                contentsArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.message)))
                    }
                )
            }
            // Append the current message
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                }
            )

            // Define tools schema
            val functionDeclarations = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "get_accounts")
                    put("description", "Retrieves all accounts in the database including names, balances, and types.")
                })
                put(JSONObject().apply {
                    put("name", "get_transactions")
                    put("description", "Retrieves all transactions in the database.")
                })
                put(JSONObject().apply {
                    put("name", "get_categories")
                    put("description", "Retrieves all transaction categories.")
                })
                put(JSONObject().apply {
                    put("name", "create_transaction")
                    put("description", "Adds a new transaction to the database. Required arguments: amount, type, categoryId, accountId. Optional: note.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("amount", JSONObject().put("type", "NUMBER"))
                            put("type", JSONObject().put("type", "STRING").put("enum", JSONArray(listOf("EXPENSE", "INCOME", "TRANSFER"))))
                            put("categoryId", JSONObject().put("type", "INTEGER"))
                            put("accountId", JSONObject().put("type", "INTEGER"))
                            put("note", JSONObject().put("type", "STRING"))
                        })
                        put("required", JSONArray(listOf("amount", "type", "categoryId", "accountId")))
                    })
                })
                put(JSONObject().apply {
                    put("name", "create_category")
                    put("description", "Creates a new transaction category or sub-category. If parentId is provided, it will be a sub-category under that parent category.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("name", JSONObject().put("type", "STRING"))
                            put("type", JSONObject().put("type", "STRING").put("enum", JSONArray(listOf("EXPENSE", "INCOME"))))
                            put("parentId", JSONObject().put("type", "INTEGER"))
                        })
                        put("required", JSONArray(listOf("name", "type")))
                    })
                })
                put(JSONObject().apply {
                    put("name", "update_transaction")
                    put("description", "Updates/modifies an existing transaction in the database. Specify the ID of the transaction and any fields you wish to change.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("id", JSONObject().put("type", "INTEGER"))
                            put("amount", JSONObject().put("type", "NUMBER"))
                            put("type", JSONObject().put("type", "STRING").put("enum", JSONArray(listOf("EXPENSE", "INCOME", "TRANSFER"))))
                            put("categoryId", JSONObject().put("type", "INTEGER"))
                            put("accountId", JSONObject().put("type", "INTEGER"))
                            put("note", JSONObject().put("type", "STRING"))
                        })
                        put("required", JSONArray(listOf("id")))
                    })
                })
                put(JSONObject().apply {
                    put("name", "delete_transaction")
                    put("description", "Deletes a transaction from the database by its ID.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("id", JSONObject().put("type", "INTEGER"))
                        })
                        put("required", JSONArray(listOf("id")))
                    })
                })
            }

            val dbContext = getDatabaseContextString()
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", """
                        أنت مساعد مالي ذكي ومحترف جداً لتطبيق قداشّ (Kdach) لإدارة المصاريف والميزانيات في الجزائر.
                        قواعد السلوك والعمل:
                        1. تفاعل مع المستخدم بلغة عربية فصحى، مهذبة، وواضحة جداً.
                        2. عند طلب المستخدم تسجيل معاملة مادية (مصروف، دخل، أو تحويل)، قم أولاً بالبحث عن الفئات الحالية باستخدام أداة `get_categories` والحسابات الحالية باستخدام أداة `get_accounts`.
                        3. طابق المعاملة مع أقرب فئة موجودة مسبقاً في التطبيق دائماً كأولوية قصوى لتجنب تكرار وتضخم الفئات بشكل غير مبرر (مثال: إذا كان النشاط 'بقالة' أو 'أكل' طابقه مع فئة 'المواد الغذائية' أو 'الطعام' المتوفرة).
                        4. لا تقم بإنشاء فئة جديدة باستخدام أداة `create_category` إلا إذا كان النشاط مختلفاً تماماً عن كل الفئات المتوفرة ولا يمكن إدراجه تحت أي منها إطلاقاً.
                        5. تصرف بمسؤولية واحترافية عالية، وقدم نصائح مالية مفيدة ومختصرة عند الحاجة.
                        6. قم بتقسيم إجاباتك المعقدة أو التحليلات التي تحتوي على أفكار متعددة أو خطوات عمل منفصلة إلى عدة فقرات/خطوات وافصل بين كل جزء والذي يليه بالرمز `[NEXT]` (مثال: 'الخطوة الأولى... [NEXT] الخطوة الثانية...'). هذا يتيح للتطبيق عرضها على شكل رسائل متسلسلة لتعزيز فهم المستخدم.
                        7. عندما يسأل المستخدم عن "الرصيد" أو "كم رصيدي" بشكل عام (دون تحديد حساب معيّن مثل CCP أو البنك)، يجب أن يكون الرد الافتراضي هو عرض إجمالي رصيد المحفظة (مجموع أرصدة كل الحسابات) أولاً كإجابة ذكية ومباشرة، مع إضافة اقتراح واضح وذكي في نهاية الرد يقترح إمكانية الاستعلام عن كل حساب على حدة، بصياغة مثل: "يمكنني أيضاً عرض تفاصيل كل حساب على حدة عند الطلب".
                        8. إذا طلب المستخدم رصيد حساب معيّن أو فئة حساب معيّنة (مثل: "رصيد CCP" أو "حساب البنك" أو "النقدي/كاش" أو "محفظتي/Wallet")، يجب عرض تفاصيل رصيد هذا الحساب المحدد فقط دون البقية.
                        
                        $dbContext
                    """.trimIndent())
                }))
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations)))
                put("systemInstruction", systemInstruction)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("X-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: java.io.IOException) {
                throw AiFailureException.NetworkFailure("فشل في الاتصال بخادم Google Gemini: ${e.localizedMessage}", e)
            }
            if (!response.isSuccessful) {
                throw AiFailureException.AiServiceFailure("فشل استجابة خادم Google Gemini برمز: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw AiFailureException.AiServiceFailure("تلقيت استجابة فارغة من خادم Google Gemini.")
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw AiFailureException.AiServiceFailure("لم يتم إنشاء استجابة بواسطة Google Gemini.")
            }

            val contentObj = candidates.getJSONObject(0).optJSONObject("content") ?: throw AiFailureException.AiServiceFailure("تلقيت ردًا فارغًا من الخادم.")
            val parts = contentObj.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                throw AiFailureException.AiServiceFailure("تلقيت ردًا فارغًا بدون محتوى نصي.")
            }
            val firstPart = parts.getJSONObject(0)

            if (firstPart.has("functionCall")) {
                val functionCall = firstPart.getJSONObject("functionCall")
                val functionName = functionCall.getString("name")
                val args = functionCall.optJSONObject("args") ?: JSONObject()

                // Execute the tool call locally
                val toolResultJson = executeToolCall(functionName, args)

                // Send response back to Gemini
                val followUpContentsArray = JSONArray().apply {
                    // Include original prompt contents
                    for (i in 0 until contentsArray.length()) {
                        put(contentsArray.getJSONObject(i))
                    }
                    // Include model function call
                    put(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().put(JSONObject().put("functionCall", functionCall)))
                    })
                    // Include tool response
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", functionName)
                                if (functionCall.has("id")) {
                                    put("id", functionCall.getString("id"))
                                }
                                put("response", JSONObject().put("content", toolResultJson.toString()))
                            })
                        }))
                    })
                }

                val followUpRequestBody = JSONObject().apply {
                    put("contents", followUpContentsArray)
                    put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations)))
                }

                val followUpRequest = Request.Builder()
                    .url(url)
                    .addHeader("X-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(followUpRequestBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val followUpResponse = try {
                    okHttpClient.newCall(followUpRequest).execute()
                } catch (e: java.io.IOException) {
                    throw AiFailureException.NetworkFailure("فشل في الاتصال بالخادم أثناء معالجة الأداة: ${e.localizedMessage}", e)
                }
                if (!followUpResponse.isSuccessful) {
                    throw AiFailureException.AiServiceFailure("فشل خادم Gemini أثناء معالجة الأداة برمز الخطأ: ${followUpResponse.code} ${followUpResponse.message}")
                }

                val followUpResponseBody = followUpResponse.body?.string() ?: ""
                val followUpResponseJson = JSONObject(followUpResponseBody)
                val finalCandidates = followUpResponseJson.optJSONArray("candidates")
                if (finalCandidates == null || finalCandidates.length() == 0) {
                    throw AiFailureException.AiServiceFailure("فشل المساعد في معالجة نتيجة الأداة.")
                }
                val finalContent = finalCandidates.getJSONObject(0).optJSONObject("content") ?: throw AiFailureException.AiServiceFailure("رد فارغ بعد استدعاء الأداة.")
                val finalParts = finalContent.optJSONArray("parts")
                if (finalParts == null || finalParts.length() == 0) {
                    throw AiFailureException.AiServiceFailure("رد فارغ بدون محتوى بعد استدعاء الأداة.")
                }
                finalParts.getJSONObject(0).optString("text", "لم يتم إرجاع أي نص.")
            } else {
                firstPart.optString("text", "لم يتم إرجاع أي نص.")
            }
        } catch (e: Exception) {
            if (e is AiFailureException) throw e
            throw AiFailureException.AiServiceFailure("حدث خطأ غير متوقع أثناء الاتصال بـ Gemini: ${e.localizedMessage}", e)
        }
    }

    private suspend fun executeToolCall(name: String, args: JSONObject): JSONObject {
        val result = JSONObject()
        try {
            when (name) {
                "get_accounts" -> {
                    val accounts = accountRepository.getAllAccounts().first()
                    val accountsArray = JSONArray()
                    for (acc in accounts) {
                        accountsArray.put(JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("balance", acc.balance)
                            put("type", acc.type.name)
                        })
                    }
                    result.put("accounts", accountsArray)
                    result.put("status", "success")
                }
                "get_transactions" -> {
                    val transactions = transactionRepository.getAllTransactions().first()
                    val transArray = JSONArray()
                    for (tr in transactions) {
                        transArray.put(JSONObject().apply {
                            put("id", tr.id)
                            put("amount", tr.amount)
                            put("type", tr.type.name)
                            put("accountId", tr.accountId)
                            put("categoryId", tr.categoryId)
                            put("note", tr.note ?: "")
                            put("date", tr.date)
                        })
                    }
                    result.put("transactions", transArray)
                    result.put("status", "success")
                }
                "get_categories" -> {
                    val categories = categoryRepository.getAllCategories().first()
                    val catArray = JSONArray()
                    for (cat in categories) {
                        catArray.put(JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("type", cat.type.name)
                        })
                    }
                    result.put("categories", catArray)
                    result.put("status", "success")
                }
                "create_transaction" -> {
                    val amount = args.getDouble("amount")
                    val typeStr = args.getString("type")
                    val categoryId = args.getLong("categoryId")
                    val accountId = args.getLong("accountId")
                    val note = args.optString("note", "")

                    val newTransaction = Transaction(
                        amount = amount,
                        type = TransactionType.valueOf(typeStr),
                        categoryId = categoryId,
                        accountId = accountId,
                        note = if (note.isEmpty()) null else note,
                        date = System.currentTimeMillis()
                    )
                    val id = transactionRepository.insertTransaction(newTransaction)
                    result.put("insertedId", id)
                    result.put("status", "success")
                }
                "create_category" -> {
                    val catName = args.getString("name")
                    val typeStr = args.getString("type")
                    val parentId = if (args.has("parentId") && !args.isNull("parentId")) args.getLong("parentId") else null
                    val newCategory = Category(
                        name = catName,
                        type = CategoryType.valueOf(typeStr),
                        icon = "📁",
                        color = "#6C63FF",
                        parentId = parentId
                    )
                    val id = categoryRepository.insertCategory(newCategory)
                    result.put("insertedId", id)
                    result.put("status", "success")
                }
                "update_transaction" -> {
                    val id = args.getLong("id")
                    val existing = transactionRepository.getTransactionById(id)
                    if (existing == null) {
                        result.put("status", "error")
                        result.put("message", "Transaction not found with ID: $id")
                    } else {
                        val amount = if (args.has("amount")) args.getDouble("amount") else existing.amount
                        val typeStr = if (args.has("type")) args.getString("type") else existing.type.name
                        val categoryId = if (args.has("categoryId")) args.getLong("categoryId") else existing.categoryId
                        val accountId = if (args.has("accountId")) args.getLong("accountId") else existing.accountId
                        val note = if (args.has("note")) {
                            val n = args.getString("note")
                            if (n.isEmpty()) null else n
                        } else existing.note

                        val updated = existing.copy(
                            amount = amount,
                            type = TransactionType.valueOf(typeStr),
                            categoryId = categoryId,
                            accountId = accountId,
                            note = note
                        )
                        transactionRepository.updateTransaction(updated)
                        result.put("status", "success")
                        result.put("updatedId", id)
                    }
                }
                "delete_transaction" -> {
                    val id = args.getLong("id")
                    transactionRepository.deleteTransactionById(id)
                    result.put("status", "success")
                    result.put("deletedId", id)
                }
                else -> {
                    result.put("status", "error")
                    result.put("message", "Unknown tool call: $name")
                }
            }
        } catch (e: Exception) {
            result.put("status", "error")
            result.put("message", e.message ?: "Unknown error")
        }
        return result
    }

    private suspend fun simulateMockAiWithTools(prompt: String): String {
        val normalized = prompt.trim().lowercase()

        // Mock error triggers for testing error banner and retry logic
        if (normalized.contains("خطأ شبكة") || normalized.contains("network error") || normalized.contains("test_network_fail")) {
            throw AiFailureException.NetworkFailure("فشل تجريبي في الاتصال بالشبكة (الوضع التجريبي).")
        }
        if (normalized.contains("خطأ خادم") || normalized.contains("ai error") || normalized.contains("test_ai_fail")) {
            throw AiFailureException.AiServiceFailure("فشل تجريبي في استجابة الذكاء الاصطناعي (الوضع التجريبي).")
        }

        // 1. Get Accounts / Balance mock trigger
        val isBalanceInquiry = normalized.contains("رصيد") || normalized.contains("الرصيد") || normalized.contains("رصيدي") || normalized.contains("balance")
        val isAccountInquiry = normalized.contains("account") || normalized.contains("حساب") || normalized.contains("الحسابات")

        if (isBalanceInquiry || isAccountInquiry) {
            val accounts = accountRepository.getAllAccounts().first()
            if (accounts.isEmpty()) {
                return "ليس لديك أي حسابات مسجلة حالياً."
            }

            // Check if user specified a particular account type or name in their query
            val matchedAccount = accounts.firstOrNull { acc ->
                val accNameLower = acc.name.lowercase()
                val accTypeLower = acc.type.name.lowercase()
                
                normalized.contains(accNameLower) || 
                normalized.contains(accTypeLower) ||
                (acc.type == AccountType.CCP && (normalized.contains("ccp") || normalized.contains("سي سي بي") || normalized.contains("البريد"))) ||
                (acc.type == AccountType.BANK && (normalized.contains("bank") || normalized.contains("بنك") || normalized.contains("البنك"))) ||
                (acc.type == AccountType.CASH && (normalized.contains("cash") || normalized.contains("كاش") || normalized.contains("نقدي") || normalized.contains("نقد"))) ||
                (acc.type == AccountType.WALLET && (normalized.contains("wallet") || normalized.contains("محفظة") || normalized.contains("المحفظة"))) ||
                (acc.type == AccountType.BARIDIMOB && (normalized.contains("baridimob") || normalized.contains("بريدي موب") || normalized.contains("بريدي"))) ||
                (acc.type == AccountType.SAVINGS && (normalized.contains("savings") || normalized.contains("توفير") || normalized.contains("ادخار")))
            }

            if (matchedAccount != null) {
                val typeLabel = when (matchedAccount.type) {
                    AccountType.BARIDIMOB -> "بريدي موب"
                    AccountType.CCP -> "CCP"
                    AccountType.CASH -> "نقدي"
                    AccountType.BANK -> "بنك"
                    AccountType.SAVINGS -> "توفير"
                    AccountType.WALLET -> "محفظة"
                    AccountType.OTHER -> "أخرى"
                }
                return "رصيد حسابك *${matchedAccount.name}* ($typeLabel) هو: **${matchedAccount.balance} ${matchedAccount.currency}**."
            } else {
                val totalBalance = accounts.sumOf { it.balance }
                val currency = accounts.firstOrNull()?.currency ?: "دج"
                
                if (isBalanceInquiry && !normalized.contains("تفاصيل") && !normalized.contains("كل") && !normalized.contains("جميع")) {
                    return "إجمالي رصيد محفظتك هو: **$totalBalance $currency**.\n\nيمكنني أيضاً عرض تفاصيل كل حساب على حدة عند الطلب."
                } else {
                    val sb = java.lang.StringBuilder("إجمالي رصيد محفظتك هو: **$totalBalance $currency**.\n\nتفاصيل الحسابات:\n")
                    accounts.forEach { acc ->
                        val typeLabel = when (acc.type) {
                            AccountType.BARIDIMOB -> "بريدي موب"
                            AccountType.CCP -> "CCP"
                            AccountType.CASH -> "نقدي"
                            AccountType.BANK -> "بنك"
                            AccountType.SAVINGS -> "توفير"
                            AccountType.WALLET -> "محفظة"
                            AccountType.OTHER -> "أخرى"
                        }
                        sb.append("- *${acc.name}* ($typeLabel): **${acc.balance} ${acc.currency}**\n")
                    }
                    return sb.toString()
                }
            }
        }

        // 2. Get Categories mock trigger
        if (normalized.contains("category") || normalized.contains("فئة") || normalized.contains("تصنيف")) {
            val categories = categoryRepository.getAllCategories().first()
            if (categories.isEmpty()) {
                return "No categories found in your database."
            }
            val sb = java.lang.StringBuilder("Here are your categories:\n")
            categories.filter { it.parentId == null }.forEach { cat ->
                sb.append("- ${cat.name} (${cat.type})\n")
            }
            return sb.toString()
        }

        // 3. Get Budget Goals mock trigger
        if (normalized.contains("budget") || normalized.contains("ميزانية") || normalized.contains("أهداف")) {
            val goals = budgetGoalRepository.getAllBudgetGoals().first()
            if (goals.isEmpty()) {
                return "You have not set any budget goals."
            }
            val sb = java.lang.StringBuilder("Here are your budget goals:\n")
            goals.forEach { goal ->
                sb.append("- Goal ID: ${goal.id}, Limit: **${goal.amountLimit} DZD**, Spent: **${goal.spentAmount} DZD**\n")
            }
            return sb.toString()
        }

        // 4. Create Transaction mock trigger
        if (normalized.contains("add") || normalized.contains("insert") || normalized.contains("إضافة") || normalized.contains("سجل")) {
            val amountRegex = "\\d+".toRegex()
            val match = amountRegex.find(normalized)
            val amount = match?.value?.toDoubleOrNull() ?: 150.0

            val accounts = accountRepository.getAllAccounts().first()
            val categories = categoryRepository.getAllCategories().first()

            val accountId = accounts.firstOrNull()?.id ?: 1L
            val categoryId = categories.firstOrNull()?.id ?: 1L

            val newTransaction = Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                accountId = accountId,
                note = "AI generated transaction from: $prompt",
                date = System.currentTimeMillis()
            )
            val id = transactionRepository.insertTransaction(newTransaction)
            return "Successfully recorded a transaction of **$amount DZD** (ID: $id) under the account *${accounts.firstOrNull()?.name ?: "Default"}* and category *${categories.firstOrNull()?.name ?: "Default"}*."
        }

        // 5. Get Transactions mock trigger
        if (normalized.contains("transaction") || normalized.contains("عملية") || normalized.contains("سجل")) {
            val transactions = transactionRepository.getAllTransactions().first().take(5)
            if (transactions.isEmpty()) {
                return "No transactions have been recorded yet."
            }
            val sb = java.lang.StringBuilder("Here are your last 5 transactions:\n")
            transactions.forEach { tr ->
                sb.append("- ID: ${tr.id}, Amount: **${tr.amount} DZD**, Type: ${tr.type}, Date: ${tr.date}\n")
            }
            return sb.toString()
        }

        // Default conversational response
        return "قداشّ — المساعد المالي الذكي: يمكنني مساعدتك في إدارة قاعدة بياناتك المالية! اسألني عن حساباتك، المعاملات، الفئات، أو قل 'أضف معاملة'."
    }

    override fun getAllSessionTitles(): Flow<List<String>> {
        return aiChatDao.getAllSessionTitles()
    }

    override suspend fun deleteSession(sessionTitle: String) {
        aiChatDao.deleteSession(sessionTitle)
    }

    override suspend fun sendCardMessage(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): String {
        val errors = mutableListOf<String>()
        for (provider in providers) {
            val result = provider.sendCardMessage(systemPrompt, history, userMessage)
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                errors.add("${provider.name}: $errMsg")
                try {
                    android.util.Log.w("AiRepository", "Provider ${provider.name} failed: $errMsg")
                } catch (e: Exception) {
                    println("AiRepository: Provider ${provider.name} failed: $errMsg")
                }
            }
        }
        throw com.example.data.repository.AiAllProvidersFailedException(
            "All AI providers failed:\n${errors.joinToString("\n")}"
        )
    }

    private suspend fun getDatabaseContextString(): String {
        return try {
            val accounts = accountRepository.getAllAccounts().first().filter { !it.isArchived }
            val categories = categoryRepository.getAllCategories().first()
            val recentTransactions = transactionRepository.getAllTransactions().first()
                .sortedByDescending { it.date }.take(20)
            val budgetGoals = budgetGoalRepository.getAllBudgetGoals().first()
            val debts = debtRepository.getAllDebts().first()

            val categoryMap = categories.associateBy { it.id }
            val accountMap = accounts.associateBy { it.id }

            val totalBalance = accounts.sumOf { it.balance }
            val accountsStr = accounts.joinToString("\n") { acc ->
                "- ${acc.name} (نوع: ${acc.type.name}, ID: ${acc.id}): الرصيد = ${acc.balance} دج"
            }

            val categoriesStr = categories.joinToString("\n") { cat ->
                "- ${cat.name} (نوع: ${cat.type.name}, ID: ${cat.id}${if (cat.parentId != null) ", أب: ${cat.parentId}" else ""})"
            }

            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
            val monthTxs = recentTransactions.filter { it.date >= thirtyDaysAgo }
            val totalIncome30 = monthTxs.filter { it.type.name == "INCOME" }.sumOf { it.amount }
            val totalExpense30 = monthTxs.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }

            val recentTxStr = recentTransactions.take(10).joinToString("\n") { tx ->
                val catName = categoryMap[tx.categoryId]?.name ?: "غير محدد"
                val accName = accountMap[tx.accountId]?.name ?: "غير محدد"
                val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                "- [${tx.type.name}] ${tx.amount} دج | فئة: $catName | حساب: $accName | تاريخ: $dateStr${if (!tx.note.isNullOrBlank()) " | ملاحظة: ${tx.note}" else ""}"
            }

            val budgetStr = if (budgetGoals.isNotEmpty()) {
                budgetGoals.filter { it.isActive }.joinToString("\n") { bg ->
                    val catName = categoryMap[bg.linkedCategoryId]?.name ?: "عام"
                    "- ميزانية ${bg.title} ($catName): الحد = ${bg.amountLimit} دج | المُنفق = ${bg.spentAmount} دج"
                }
            } else "لا توجد ميزانيات محددة"

            val debtsStr = if (debts.isNotEmpty()) {
                val activeDebts = debts.filter { !it.isClosed }
                if (activeDebts.isEmpty()) "لا توجد ديون نشطة"
                else activeDebts.joinToString("\n") { d ->
                    "- ${d.title} (${d.creditorName}): المتبقي = ${d.remainingAmount} دج من ${d.totalAmount} دج"
                }
            } else "لا توجد ديون"

            """
            ═══════════════════════════════════════
            سياق قاعدة بيانات المستخدم الكاملة — لديك صلاحية قراءة كاملة لجميع البيانات التالية واستخدامها للإجابة بدقة:
            ═══════════════════════════════════════

            💰 إجمالي رصيد المحفظة: $totalBalance دج

            📊 إحصائيات آخر 30 يوم:
            - إجمالي الدخل: $totalIncome30 دج
            - إجمالي المصاريف: $totalExpense30 دج
            - الفارق (الادخار): ${totalIncome30 - totalExpense30} دج

            🏦 الحسابات المتوفرة وأرصدتها:
            $accountsStr

            🗂️ آخر 10 معاملات:
            $recentTxStr

            📑 الفئات المتوفرة:
            $categoriesStr

            🎯 الميزانيات:
            $budgetStr

            💳 الديون:
            $debtsStr
            ═══════════════════════════════════════
            """.trimIndent()
        } catch (e: Exception) {
            ""
        }
    }

    private fun buildOpenAiToolsJson(): JSONArray {
        val tools = JSONArray()
        
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "get_accounts")
                put("description", "Retrieves all accounts in the database including names, balances, and types.")
            })
        })
        
        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "get_transactions")
                put("description", "Retrieves all transactions in the database.")
            })
        })

        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "get_categories")
                put("description", "Retrieves all transaction categories.")
            })
        })

        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "create_transaction")
                put("description", "Adds a new transaction to the database. Required arguments: amount, type, categoryId, accountId. Optional: note.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("amount", JSONObject().put("type", "number"))
                        put("type", JSONObject().put("type", "string").put("enum", JSONArray(listOf("EXPENSE", "INCOME", "TRANSFER"))))
                        put("categoryId", JSONObject().put("type", "integer"))
                        put("accountId", JSONObject().put("type", "integer"))
                        put("note", JSONObject().put("type", "string"))
                    })
                    put("required", JSONArray(listOf("amount", "type", "categoryId", "accountId")))
                })
            })
        })

        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "update_transaction")
                put("description", "Updates/modifies an existing transaction in the database. Specify the ID of the transaction and any fields you wish to change.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("id", JSONObject().put("type", "integer"))
                        put("amount", JSONObject().put("type", "number"))
                        put("type", JSONObject().put("type", "string").put("enum", JSONArray(listOf("EXPENSE", "INCOME", "TRANSFER"))))
                        put("categoryId", JSONObject().put("type", "integer"))
                        put("accountId", JSONObject().put("type", "integer"))
                        put("note", JSONObject().put("type", "string"))
                    })
                    put("required", JSONArray(listOf("id")))
                })
            })
        })

        tools.put(JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "create_category")
                put("description", "Creates a new transaction category or sub-category. If parentId is provided, it will be a sub-category under that parent category.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("name", JSONObject().put("type", "string"))
                        put("type", JSONObject().put("type", "string").put("enum", JSONArray(listOf("EXPENSE", "INCOME"))))
                        put("parentId", JSONObject().put("type", "integer"))
                    })
                    put("required", JSONArray(listOf("name", "type")))
                })
            })
        })

        return tools
    }
}

class AiAllProvidersFailedException(message: String) : Exception(message)

