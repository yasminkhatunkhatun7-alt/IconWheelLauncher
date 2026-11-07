package com.kyro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kyro.AppModule
import com.kyro.KyroApplication
import com.kyro.control.PhoneticMatcher

class WakeWordService : Service() {

    private lateinit var appModule: AppModule
    private var speechRecognizer: SpeechRecognizer? = null
    private val phoneticMatcher = PhoneticMatcher()

    private var isAwaitingConfirmation = false
    private var customWakeWord = "kyro"

    override fun onCreate() {
        super.onCreate()
        appModule = (application as KyroApplication).appModule
        createNotificationChannel()

        customWakeWord = appModule.sharedPreferences.getString("customWakeWord", "kyro") ?: "kyro"

        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                setupListener()
            }
        } catch (e: Exception) {
            Log.e("KYRO_WAKE", "Failed to init SpeechRecognizer", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "kyro_wake")
            .setContentTitle("KYRO Core")
            .setContentText("Listening for wake word: $customWakeWord")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        try { startForeground(2, notification) } catch (e: Exception) { Log.e("KYRO_WAKE", "startForeground failed", e) }
        startListening()

        return START_STICKY
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // Restart listening continuously
                startListening()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    processSpokenText(text)
                }
                startListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processSpokenText(text: String) {
        if (isAwaitingConfirmation) {
            val lowerText = text.lowercase()
            if (lowerText.contains("yes") || lowerText.contains("ha") || lowerText.contains("haan")) {
                isAwaitingConfirmation = false
                triggerMainApp()
            } else {
                isAwaitingConfirmation = false
            }
            return
        }

        // Feature 4: Smart Wake Word with Phonetic Matching
        val matchScore = phoneticMatcher.match(text, customWakeWord)

        if (matchScore >= 0.8f) {
            triggerMainApp()
        } else if (matchScore >= 0.5f) {
            isAwaitingConfirmation = true
            appModule.ttsEngine.speak("Kya aapne $customWakeWord bola?")
        }
    }

    private fun triggerMainApp() {
        appModule.soundHelper.playWakeBeep()
        val intent = Intent(this, com.kyro.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // A special flag to let MainActivity know to open VoiceScreen directly
        intent.putExtra("OPEN_VOICE_MODE", true)
        startActivity(intent)
    }

    private fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("KYRO_WAKE", "Start listening failed", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "kyro_wake",
                "Wake Word Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
