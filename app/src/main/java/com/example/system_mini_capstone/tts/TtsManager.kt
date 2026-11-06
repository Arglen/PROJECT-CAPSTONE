package com.example.system_mini_capstone.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Encapsulates the Android TextToSpeech engine with initialization safety and debounced speech output.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                    engine.setSpeechRate(0.95f)
                    Log.i(TAG, "TextToSpeech engine initialized successfully")
                } else {
                    Log.w(TAG, "Language US not supported or missing data")
                }
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String) {
        if (!isInitialized || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
