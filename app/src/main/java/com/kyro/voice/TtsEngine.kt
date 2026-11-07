package com.kyro.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("KYRO_TTS", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("KYRO_TTS", "Language en-IN not supported")
            } else {
                isInitialized = true
                tts?.setPitch(0.85f) // Default Jarvis-style pitch

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        abandonAudioFocus()
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        abandonAudioFocus()
                    }
                })
            }
        } else {
            Log.e("KYRO_TTS", "TTS Initialization failed")
        }
    }

    fun speak(text: String, rmsLevel: Float = 0f) {
        if (!isInitialized) return

        requestAudioFocus()

        // Feature 8: Emotion Detection Pitch adjustment
        // If RMS is extremely high, user is likely shouting. Lower pitch to sound calm.
        if (rmsLevel > 10f) {
            tts?.setPitch(0.6f)
        } else {
            tts?.setPitch(0.85f)
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "KYRO_UTTERANCE")
        }

        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "KYRO_UTTERANCE")
        } catch (e: Exception) {
            Log.e("KYRO_TTS", "Speak failed", e)
            abandonAudioFocus()
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            abandonAudioFocus()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()

            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
