package com.qdash.data.ai.providers

import com.qdash.data.ai.AiProvider
import com.qdash.data.ai.buildOpenAiRequestBody
import com.qdash.data.ai.parseOpenAiResponse
import com.qdash.domain.model.AiChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OpenCodeProvider(
    private val apiKey: String,
    private val parentClient: OkHttpClient
) : AiProvider {
    override val name = "OpenCode"

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
            return@withContext Result.failure(IllegalArgumentException("OpenCode API key is empty"))
        }

        val models = listOf(
            "nemotron-3-ultra-free"
        )

        val errors = mutableListOf<String>()
        for (modelId in models) {
            try {
                val body = buildOpenAiRequestBody(systemPrompt, history, userMessage, modelId)
                val request = Request.Builder()
                    .url("https://opencode.ai/zen/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val reply = parseOpenAiResponse(response)
                return@withContext Result.success(reply)
            } catch (e: Exception) {
                errors.add("Model $modelId failed: ${e.localizedMessage}")
            }
        }
        Result.failure(Exception("All OpenCode models failed:\n${errors.joinToString("\n")}"))
    }
}
