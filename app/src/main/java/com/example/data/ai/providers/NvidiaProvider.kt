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

class NvidiaProvider(
    private val apiKey: String,
    private val parentClient: OkHttpClient
) : AiProvider {
    override val name = "NVIDIA NIM"

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
            return@withContext Result.failure(IllegalArgumentException("NVIDIA API key is empty"))
        }

        val models = listOf(
            "meta/llama-3.1-8b-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "meta/llama-3.3-70b-instruct"
        )

        val errors = mutableListOf<String>()
        for (modelId in models) {
            try {
                val body = buildOpenAiRequestBody(systemPrompt, history, userMessage, model = modelId)
                val request = Request.Builder()
                    .url("https://integrate.api.nvidia.com/v1/chat/completions")
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
        Result.failure(Exception("All NVIDIA models failed:\n${errors.joinToString("\n")}"))
    }
}
