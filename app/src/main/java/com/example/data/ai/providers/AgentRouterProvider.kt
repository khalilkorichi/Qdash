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

class AgentRouterProvider(
    private val apiKey: String,
    private val parentClient: OkHttpClient
) : AiProvider {
    override val name = "AgentRouter"

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
            return@withContext Result.failure(IllegalArgumentException("AgentRouter API key is empty"))
        }

        val models = listOf(
            "qwen-3-coder",
            "llama-3.3-70b",
            "glm-5.1"
        )

        val errors = mutableListOf<String>()
        for (modelId in models) {
            try {
                val body = buildOpenAiRequestBody(systemPrompt, history, userMessage, modelId)
                val request = Request.Builder()
                    .url("https://agentrouter.org/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Originator", "codex_cli_rs")
                    .addHeader("User-Agent", "codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464")
                    .addHeader("Version", "0.101.0")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val reply = parseOpenAiResponse(response)
                return@withContext Result.success(reply)
            } catch (e: Exception) {
                errors.add("Model $modelId failed: ${e.localizedMessage}")
            }
        }
        Result.failure(Exception("All AgentRouter models failed:\n${errors.joinToString("\n")}"))
    }
}
