package com.kyro.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.Calendar

class KyroAccessibilityService : AccessibilityService() {

    private var volumeDownPressCount = 0
    private val resetHandler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { volumeDownPressCount = 0 }

    private val isNightModeEnabled = true
    private val nightModeStartHour = 22
    private val nightModeEndHour = 6
    private val emergencyContact = "1234567890"

    // WhatsApp State Machine
    private enum class WhatsAppState { IDLE, SEARCHING_CONTACT, TYPING_MESSAGE }
    private var waState = WhatsAppState.IDLE
    private var pendingWhatsAppContact: String? = null
    private var pendingWhatsAppMessage: String? = null

    private val automationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.kyro.ACTION_AUTOMATION") {
                val actionType = intent.getStringExtra("action_type")
                val payload = intent.getStringExtra("payload") ?: ""

                when (actionType) {
                    "SCROLL_DOWN" -> {
                        performScrollDown()
                        Log.d("KYRO_ACC", "Scroll Down Requested")
                    }
                    "READ_SCREEN" -> {
                        readScreenText()
                    }
                    "CLICK_TEXT" -> {
                        clickOnText(payload)
                    }
                    "WHATSAPP_AUTOMATION" -> {
                        val parts = payload.split("|")
                        if (parts.size == 2) {
                            pendingWhatsAppContact = parts[0]
                            pendingWhatsAppMessage = parts[1]
                            waState = WhatsAppState.SEARCHING_CONTACT
                            launchWhatsApp()
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED or
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info

        val filter = IntentFilter("com.kyro.ACTION_AUTOMATION")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(automationReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(automationReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            if (isNightModeEnabled && isNightTime()) {
                val packageName = event.packageName?.toString()
                if (packageName != null &&
                    packageName != "com.kyro" &&
                    packageName != "com.android.dialer" &&
                    packageName != "com.android.systemui" &&
                    packageName != "com.whatsapp") {

                    Log.d("KYRO_ACCESSIBILITY", "Blocking app: $packageName during Night Mode")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }

            if (waState != WhatsAppState.IDLE && event.packageName == "com.whatsapp") {
                handleWhatsAppAutomation(rootInActiveWindow)
            }
        } catch (e: Exception) {
            Log.e("KYRO_ACCESSIBILITY", "Error processing accessibility event", e)
        }
    }

    private fun performScrollDown() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val middleX = displayMetrics.widthPixels / 2f
            val startY = displayMetrics.heightPixels * 0.8f
            val endY = displayMetrics.heightPixels * 0.2f

            val path = Path().apply {
                moveTo(middleX, startY)
                lineTo(middleX, endY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()

            dispatchGesture(gesture, null, null)
        }
    }

    private fun readScreenText() {
        val root = rootInActiveWindow ?: return
        val textBuilder = StringBuilder()

        fun traverse(node: AccessibilityNodeInfo) {
            if (node.text != null) {
                textBuilder.append(node.text).append(" ")
            } else if (node.contentDescription != null) {
                textBuilder.append(node.contentDescription).append(" ")
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it) }
            }
        }
        traverse(root)
        // Ideally send this text back to the app/TTS. For now we toast it to show it worked.
        Toast.makeText(this, "Screen: ${textBuilder.toString().take(50)}...", Toast.LENGTH_SHORT).show()
    }

    private fun clickOnText(text: String) {
        val root = rootInActiveWindow ?: return
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            } else if (node.parent?.isClickable == true) {
                node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    private fun launchWhatsApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("KYRO_ACC", "Failed to open WA", e)
            waState = WhatsAppState.IDLE
        }
    }

    private fun handleWhatsAppAutomation(node: AccessibilityNodeInfo?) {
        if (node == null) return

        try {
            if (waState == WhatsAppState.SEARCHING_CONTACT) {
                // Step 1: Click Search icon
                val searchNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/menuitem_search")
                if (searchNodes.isNotEmpty()) {
                    searchNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }

                // Step 2: Type contact name
                val searchInputNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/search_src_text")
                if (searchInputNodes.isNotEmpty()) {
                    val arguments = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingWhatsAppContact)
                    }
                    searchInputNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    // Give it a moment to filter contacts, then find and click the contact
                    Handler(Looper.getMainLooper()).postDelayed({
                        val refreshedRoot = rootInActiveWindow ?: return@postDelayed
                        val contactNodes = refreshedRoot.findAccessibilityNodeInfosByText(pendingWhatsAppContact)
                        for (contactNode in contactNodes) {
                            // Avoid clicking the search bar text again
                            if (contactNode.viewIdResourceName != "com.whatsapp:id/search_src_text") {
                                if (contactNode.isClickable) {
                                    contactNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    waState = WhatsAppState.TYPING_MESSAGE
                                    return@postDelayed
                                } else if (contactNode.parent?.isClickable == true) {
                                    contactNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    waState = WhatsAppState.TYPING_MESSAGE
                                    return@postDelayed
                                }
                            }
                        }
                    }, 1000)
                }
            } else if (waState == WhatsAppState.TYPING_MESSAGE) {
                // Step 4: Type Message
                val messageInputNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
                if (messageInputNodes.isNotEmpty()) {
                    val arguments = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingWhatsAppMessage)
                    }
                    messageInputNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    // Step 5: Send
                    Handler(Looper.getMainLooper()).postDelayed({
                        val refreshedRoot = rootInActiveWindow ?: return@postDelayed
                        val sendNodes = refreshedRoot.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                        if (!sendNodes.isNullOrEmpty()) {
                            sendNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            // Finish
                            waState = WhatsAppState.IDLE
                            pendingWhatsAppContact = null
                            pendingWhatsAppMessage = null
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                    }, 500)
                }
            }
        } catch (e: Exception) {
            Log.e("KYRO_ACC", "WA automation failed", e)
            waState = WhatsAppState.IDLE
        }
    }

    override fun onInterrupt() {
        Log.e("KYRO_ACCESSIBILITY", "Service Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            volumeDownPressCount++

            resetHandler.removeCallbacks(resetRunnable)
            resetHandler.postDelayed(resetRunnable, 2000)

            if (volumeDownPressCount == 3) {
                triggerSOS()
                volumeDownPressCount = 0
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun triggerSOS() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.vibrate(500)

            val smsManager = SmsManager.getDefault()
            val locationUrl = "http://maps.google.com/?q=0,0"
            smsManager.sendTextMessage(emergencyContact, null, "SOS! I need help. Location: $locationUrl", null, null)

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(automationReceiver)
    }
}
