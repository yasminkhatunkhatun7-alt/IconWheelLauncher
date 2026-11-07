package com.kyro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kyro.ui.home.HomeScreen
import com.kyro.ui.SplashScreen
import androidx.compose.runtime.*
import com.kyro.ui.theme.KyroTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            startBackgroundServices()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openVoiceMode = intent.getBooleanExtra("OPEN_VOICE_MODE", false)

        checkAndRequestPermissions()

        val appModule = (application as KyroApplication).appModule

        setContent {
            KyroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    appModule.sharedPreferences.edit().putBoolean("start_in_voice", openVoiceMode).apply()
                    var showSplash by remember { mutableStateOf(true) }
                    if (showSplash) {
                        SplashScreen { showSplash = false }
                    } else {
                        HomeScreen(appModule = appModule)
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Already have permissions
            startBackgroundServices()
        }
    }

    private fun startBackgroundServices() {
        try {
            val startWakeIntent = Intent(this, com.kyro.service.WakeWordService::class.java)
            ContextCompat.startForegroundService(this, startWakeIntent)
        } catch (e: Exception) {
            Log.e("KYRO", "Failed to start WakeWordService", e)
        }

        try {
            ContextCompat.startForegroundService(this, Intent(this, com.kyro.service.ProactiveService::class.java))
        } catch (e: Exception) {
             Log.e("KYRO", "Failed to start ProactiveService", e)
        }
    }
}
