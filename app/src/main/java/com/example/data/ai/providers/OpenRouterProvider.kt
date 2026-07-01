package com.example.data.ai.providers

import com.example.data.ai.AiProvider
import com.example.data.ai.buildOpenAiRequestBody
import com.example.data.ai.parseOpenAiResponse
import com.example.domain.model.AiChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OpenRouterProvider(
    private val apiKey: String,
    private val parentClient: OkHttpClient
) : AiProvider {
    override val name = "OpenRouter"

    private val client = parentClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun sendCardMessage(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenRouter API key is empty"))
        }

        val models = listOf(
            "google/gemini-2.5-pro",
            "google/gemini-2.5-flash:free",
            "google/gemini-2.5-flash",
            "openai/gpt-4o-mini"
        )

        val errors = mutableListOf<String>()
        for (modelId in models) {
            try {
                val body = buildOpenAiRequestBody(systemPrompt, history, userMessage, modelId)
                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://github.com/khalilkorichi/Qdash")
                    .addHeader("X-Title", "Qdash")
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                val reply = parseOpenAiResponse(response)
                return@withContext Result.success(reply)
            } catch (e: Exception) {
                errors.add("Model $modelId failed: ${e.localizedMessage}")
            }
        }
        Result.failure(Exception("All OpenRouter models failed:\n${errors.joinToString("\n")}"))
    }
}
