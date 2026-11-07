package com.kyro.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

class KyroAccessibilityService : AccessibilityService() {

    private var volumeDownPressCount = 0
    private val resetHandler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { volumeDownPressCount = 0 }

    // Configuration values (can be synced with SharedPreferences later)
    private val isNightModeEnabled = true
    private val nightModeStartHour = 22 // 10 PM
    private val nightModeEndHour = 6    // 6 AM
    private val emergencyContact = "1234567890"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // --- Feature: App Blocker (Night Mode) ---
        if (isNightModeEnabled && isNightTime()) {
            val packageName = event.packageName?.toString()
            if (packageName != null &&
                packageName != "com.kyro" &&
                packageName != "com.android.dialer" &&
                packageName != "com.android.systemui") {

                // Block the app by pressing home
                Log.d("KYRO_ACCESSIBILITY", "Blocking app: $packageName during Night Mode")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }

        // --- Feature: WhatsApp Automation (Stub) ---
        // Implementation for finding nodes by text/content description
        // and typing text using AccessibilityNodeInfo goes here.
    }

    override fun onInterrupt() {
        Log.e("KYRO_ACCESSIBILITY", "Service Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // --- Feature: SOS Panic Mode ---
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            volumeDownPressCount++

            // Cancel the previous reset timer and start a new one
            resetHandler.removeCallbacks(resetRunnable)
            resetHandler.postDelayed(resetRunnable, 2000)

            if (volumeDownPressCount == 3) {
                triggerSOS()
                volumeDownPressCount = 0
                return true // Consume the event
            }
        }
        return super.onKeyEvent(event)
    }

    private fun triggerSOS() {
        try {
            Log.d("KYRO_ACCESSIBILITY", "SOS Triggered!")
            // 1. Vibrate once (Assuming VIBRATOR_SERVICE is available)
            val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.vibrate(500)

            // 2. Send SMS
            val smsManager = SmsManager.getDefault()
            val locationUrl = "http://maps.google.com/?q=0,0" // Stub for real GPS logic
            smsManager.sendTextMessage(emergencyContact, null, "SOS! I need help. Location: $locationUrl", null, null)

            // 3. WhatsApp Message (Intent)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$emergencyContact&text=SOS! I need help.")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e("KYRO_ACCESSIBILITY", "Failed to trigger SOS", e)
        }
    }

    private fun isNightTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= nightModeStartHour || hour < nightModeEndHour
    }
}
