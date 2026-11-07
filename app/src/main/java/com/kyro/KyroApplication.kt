package com.kyro

import android.app.Application
import android.util.Log

class KyroApplication : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("KYRO", "Application initialized")

        try {
            appModule = AppModule(this)

            // Initialize TTS and DB eagerly
            appModule.ttsEngine
            appModule.brainQuery.writableDatabase
        } catch (e: Exception) {
            Log.e("KYRO", "Initialization failed", e)
        }
    }
}
