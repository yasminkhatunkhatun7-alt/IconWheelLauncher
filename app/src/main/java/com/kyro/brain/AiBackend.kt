package com.kyro.brain

interface AiBackend {
    var serverUrl: String
    suspend fun sendQuery(prompt: String, contextLog: List<Pair<String, String>>): String
}
