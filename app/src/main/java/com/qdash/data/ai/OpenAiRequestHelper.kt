package com.qdash.data.ai

import com.qdash.domain.model.AiChatMessage
import com.qdash.domain.model.ChatSender
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

fun buildOpenAiRequestBody(
    systemPrompt: String,
    history: List<AiChatMessage>,
    userMessage: String,
    model: String
): RequestBody {
    val messagesArray = JSONArray()
    messagesArray.put(
        JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        }
    )
    for (msg in history) {
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
            put("content", userMessage)
        }
    )
    val json = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("max_tokens", 1024)
    }
    return json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
}

fun parseOpenAiResponse(response: Response): String {
    val body = response.body?.string() ?: throw IOException("Empty response body")
    if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $body")
    val json = JSONObject(body)
    val choices = json.optJSONArray("choices") ?: throw IOException("No choices generated in response")
    if (choices.length() == 0) throw IOException("Empty choices array")
    return choices
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
}
