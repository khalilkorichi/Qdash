package com.example.data.ai.providers

import com.example.data.ai.AiProvider
import com.example.domain.model.AiChatMessage
import com.example.domain.model.ChatSender
import com.example.domain.model.AiFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val apiKey: String,
    private val parentClient: OkHttpClient
) : AiProvider {
    override val name = "Gemini"

    private val client = parentClient.newBuilder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
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
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-pro",
            "gemini-1.5-flash",
            "gemini-1.0-pro",
            "gemini-1.0-flash"
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
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        
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
