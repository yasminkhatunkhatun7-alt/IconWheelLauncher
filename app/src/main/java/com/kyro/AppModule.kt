package com.kyro

import android.content.Context
import android.content.SharedPreferences
import com.kyro.brain.RawBrainQuery
import com.kyro.brain.TermuxServerClient
import com.kyro.control.CommandParser
import com.kyro.control.ContextMemory
import com.kyro.voice.SpeechInput
import com.kyro.voice.SoundHelper
import com.kyro.voice.TtsEngine

class AppModule(private val context: Context) {
    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("KyroPrefs", Context.MODE_PRIVATE)
    }

    val termuxServerClient by lazy {
        val client = TermuxServerClient()
        client.serverUrl = sharedPreferences.getString("serverUrl", "http://127.0.0.1:8080/completion") ?: "http://127.0.0.1:8080/completion"
        client
    }

    fun updateWakeWord(word: String) {
        sharedPreferences.edit().putString("customWakeWord", word).apply()
    }

    fun updateServerUrl(url: String) {
        sharedPreferences.edit().putString("serverUrl", url).apply()
        termuxServerClient.serverUrl = url
    }

    val brainQuery by lazy { RawBrainQuery(context) }
    val contextMemory by lazy { ContextMemory() }
    val commandParser by lazy { CommandParser(context, brainQuery, termuxServerClient, contextMemory) }

    val ttsEngine by lazy { TtsEngine(context) }
    val soundHelper by lazy { SoundHelper(context) }
    val speechInput by lazy { SpeechInput(context, soundHelper) }
}
