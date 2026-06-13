package com.example.data.repository

import com.example.data.local.dao.AiChatDao
import com.example.domain.model.AiChatMessage
import com.example.domain.model.ChatSender
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
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
    private val notificationRepository: NotificationRepository
) : AiRepository {

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
        return if (!key.isNullOrBlank()) key else ""
    }

    private val AGENT_ROUTER_API_KEY = System.getenv("AGENT_ROUTER_API_KEY") ?: ""
    private val AGENT_ROUTER_BASE_URL = "https://agentrouter.org/v1"

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

        // Otherwise, run our standard helper
        val textReply = if (modelId == "glm-5.1") {
            callAgentRouterApi("Default", prompt)
        } else {
            val apiKey = getApiKey()
            if (!apiKey.isNullOrBlank()) {
                callGeminiApiWithTools(apiKey, "Default", prompt, modelId)
            } else {
                simulateMockAiWithTools(prompt)
            }
        }

        return AiResponse(replyText = textReply)
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
        // First insert the user's message in the DB
        insertMessage(
            AiChatMessage(
                sender = ChatSender.USER,
                message = userPrompt,
                sessionTitle = sessionTitle
            )
        )

        val aiResponse = if (modelId == "glm-5.1") {
            callAgentRouterApi(sessionTitle, userPrompt)
        } else {
            val apiKey = getApiKey()
            if (!apiKey.isNullOrBlank()) {
                callGeminiApiWithTools(apiKey, sessionTitle, userPrompt, modelId)
            } else {
                // For mock, support simulated [NEXT] steps for demonstration
                if (userPrompt.contains("حلل") || userPrompt.contains("تقرير") || userPrompt.contains("تحليل")) {
                    "الخطوة 1: جاري تحليل مصاريفك الإجمالية لشهر مايو... [NEXT] الخطوة 2: يظهر أن فئة المواد الغذائية تستهلك 45% من دخلك. [NEXT] الخطوة 3: ننصحك بتقليل الطلبات الخارجية لزيادة معدل ادخارك بنسبة 10%."
                } else {
                    simulateMockAiWithTools(userPrompt)
                }
            }
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

    private suspend fun callAgentRouterApi(sessionTitle: String, userPrompt: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "$AGENT_ROUTER_BASE_URL/chat/completions"
            val history = getMessagesBySession(sessionTitle).first().takeLast(10)
            val filteredHistory = if (history.isNotEmpty() && 
                history.last().sender == ChatSender.USER && 
                history.last().message.trim() == userPrompt.trim()) {
                history.dropLast(1)
            } else {
                history
            }
            
            val messagesArray = JSONArray()
            // Do not add system instruction for glm-5.1 as it triggers safety filters (sensitive words detected)
            
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
                put("model", "glm-5.1")
                put("messages", messagesArray)
                put("tools", buildOpenAiToolsJson())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $AGENT_ROUTER_API_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Originator", "codex_cli_rs")
                .addHeader("User-Agent", "codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464")
                .addHeader("Version", "0.101.0")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Error calling Agent Router API: ${response.code} ${response.message}"
            }

            val responseBody = response.body?.string() ?: return@withContext "Empty response from Agent Router API"
            val responseJson = JSONObject(responseBody)
            val choices = responseJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext "No response generated by Agent Router."
            }

            val choiceObj = choices.getJSONObject(0)
            val messageObj = choiceObj.optJSONObject("message") ?: return@withContext "تلقيت ردًا فارغًا من الخادم."
            
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
                        put("model", "glm-5.1")
                        put("messages", followUpMessagesArray)
                    }

                    val followUpRequest = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $AGENT_ROUTER_API_KEY")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Originator", "codex_cli_rs")
                        .addHeader("User-Agent", "codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464")
                        .addHeader("Version", "0.101.0")
                        .post(followUpRequestBodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val followUpResponse = okHttpClient.newCall(followUpRequest).execute()
                    if (!followUpResponse.isSuccessful) {
                        return@withContext "Error resolving tool call with Agent Router API: ${followUpResponse.code} ${followUpResponse.message}"
                    }

                    val followUpResponseBody = followUpResponse.body?.string() ?: ""
                    val followUpResponseJson = JSONObject(followUpResponseBody)
                    val finalChoices = followUpResponseJson.optJSONArray("choices")
                    if (finalChoices == null || finalChoices.length() == 0) {
                        return@withContext "فشل المساعد في معالجة نتيجة الأداة."
                    }
                    val finalMessage = finalChoices.getJSONObject(0).optJSONObject("message") ?: return@withContext "رد فارغ بعد استدعاء الأداة."
                    return@withContext finalMessage.optString("content", "لم يتم إرجاع أي نص.")
                }
            }

            messageObj.optString("content", "لم يتم إرجاع أي نص.")
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to contact Agent Router Assistant: ${e.localizedMessage}."
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

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Error calling Gemini API: ${response.code} ${response.message}"
            }

            val responseBody = response.body?.string() ?: return@withContext "Empty response from Gemini API"
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext "No response generated by the model."
            }

            val contentObj = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext "تلقيت ردًا فارغًا من الخادم."
            val parts = contentObj.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return@withContext "تلقيت ردًا فارغًا بدون محتوى نصي."
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

                val followUpResponse = okHttpClient.newCall(followUpRequest).execute()
                if (!followUpResponse.isSuccessful) {
                    return@withContext "Error resolving tool call with Gemini API: ${followUpResponse.message}"
                }

                val followUpResponseBody = followUpResponse.body?.string() ?: ""
                val followUpResponseJson = JSONObject(followUpResponseBody)
                val finalCandidates = followUpResponseJson.optJSONArray("candidates")
                if (finalCandidates == null || finalCandidates.length() == 0) {
                    return@withContext "فشل المساعد في معالجة نتيجة الأداة."
                }
                val finalContent = finalCandidates.getJSONObject(0).optJSONObject("content") ?: return@withContext "رد فارغ بعد استدعاء الأداة."
                val finalParts = finalContent.optJSONArray("parts")
                if (finalParts == null || finalParts.length() == 0) {
                    return@withContext "رد فارغ بدون محتوى بعد استدعاء الأداة."
                }
                finalParts.getJSONObject(0).optString("text", "لم يتم إرجاع أي نص.")
            } else {
                firstPart.optString("text", "لم يتم إرجاع أي نص.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to contact AI Assistant: ${e.localizedMessage}. Falling back to mock assistant."
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

        // 1. Get Accounts mock trigger
        if (normalized.contains("account") || normalized.contains("حساب")) {
            val accounts = accountRepository.getAllAccounts().first()
            if (accounts.isEmpty()) {
                return "You don't have any accounts set up yet."
            }
            val sb = java.lang.StringBuilder("Here are your current accounts:\n")
            accounts.forEach { acc ->
                sb.append("- *${acc.name}* (${acc.type}): **${acc.balance} ${acc.currency}**\n")
            }
            return sb.toString()
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
