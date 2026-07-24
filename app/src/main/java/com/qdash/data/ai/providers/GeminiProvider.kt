package com.qdash.data.ai.providers

import com.qdash.data.ai.AiProvider
import com.qdash.domain.model.AiChatMessage
import com.qdash.domain.model.ChatSender
import com.qdash.domain.model.AiFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

fun parseGeminiModelConfig(modelId: String): Pair<String, Int?> {
    return when (modelId) {
        "gemini-3.6-flash-high" -> Pair("gemini-3.6-flash", 8192)
        "gemini-3.6-flash-medium" -> Pair("gemini-3.6-flash", 4096)
        "gemini-3.6-flash-low" -> Pair("gemini-3.6-flash", 1024)
        "gemini-3.5-flash-lite" -> Pair("gemini-3.5-flash-lite", null)
        "gemini-3.1-flash" -> Pair("gemini-3.5-flash", null)
        "gemini-3.1-pro" -> Pair("gemini-3.1-pro-preview", null)
        else -> Pair(modelId, null)
    }
}

class GeminiProvider(
    private val apiKey: String,
    private val parentClient: OkHttpClient
) : AiProvider {
    override val name = "Gemini"

    private val client = parentClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun sendCardMessage(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty"))
        }

        val models = listOf(
            "gemini-3.6-flash-high",
            "gemini-3.6-flash-medium",
            "gemini-3.6-flash-low",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )

        val errors = mutableListOf<String>()
        for (modelId in models) {
            try {
                val reply = callGeminiApiForCard(systemPrompt, history, userMessage, modelId)
                return@withContext Result.success(reply)
            } catch (e: Exception) {
                errors.add("Model $modelId failed: ${e.localizedMessage}")
            }
        }
        Result.failure(Exception("All Gemini models failed:\n${errors.joinToString("\n")}"))
    }

    private fun callGeminiApiForCard(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String,
        modelId: String
    ): String {
        val (apiModelId, thinkingBudget) = parseGeminiModelConfig(modelId)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$apiModelId:generateContent?key=$apiKey"
        
        val contentsArray = JSONArray()
        for (msg in history) {
            val role = if (msg.sender == ChatSender.USER) "user" else "model"
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.message)))
                }
            )
        }
        // Append user message
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            }
        )
        
        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
        }
        
        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", systemInstruction)
            if (thinkingBudget != null) {
                put("generationConfig", JSONObject().apply {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingBudget", thinkingBudget)
                    })
                })
            }
        }
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()
            
        val response = try {
            client.newCall(request).execute()
        } catch (e: java.io.IOException) {
            throw AiFailureException.NetworkFailure("Network call failed: ${e.localizedMessage}", e)
        }
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            throw AiFailureException.AiServiceFailure("Gemini API call failed with code: ${response.code}. Details: $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw AiFailureException.AiServiceFailure("Empty response body")
        val responseJson = JSONObject(responseBody)
        val candidates = responseJson.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw AiFailureException.AiServiceFailure("No candidates generated")
        }
        
        val contentObj = candidates.getJSONObject(0).optJSONObject("content") ?: throw AiFailureException.AiServiceFailure("Empty content object")
        val parts = contentObj.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw AiFailureException.AiServiceFailure("No parts in content")
        }
        
        return parts.getJSONObject(0).optString("text", "")
    }
}
