package com.kyro.brain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TermuxServerClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    var serverUrl: String = "http://127.0.0.1:8080/completion"

    suspend fun sendQuery(prompt: String, contextLog: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        try {
            // Feature 1: RAG Context Injection
            val contextString = if (contextLog.isNotEmpty()) {
                "Context of past conversations:\n" + contextLog.joinToString("\n") {
                    "User previously asked [${it.first}] and I answered [${it.second}]"
                } + "\n\n"
            } else ""

            val finalPrompt = "${contextString}You are KYRO, a quiet, powerful AI assistant. Answer briefly. User says: $prompt"

            val jsonPayload = JSONObject().apply {
                put("prompt", finalPrompt)
                put("n_predict", 100)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url(serverUrl)
                .post(jsonPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    return@withContext jsonResponse.optString("content", "Data nahi hai.")
                }
            }
            return@withContext "Data nahi hai."
        } catch (e: Exception) {
            Log.e("KYRO_TERMUX", "Server error", e)
            return@withContext "Data nahi hai."
        }
    }
}
