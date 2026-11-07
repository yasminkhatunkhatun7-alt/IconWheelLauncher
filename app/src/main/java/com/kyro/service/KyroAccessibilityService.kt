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

    // Feature 16: App Blocker Custom Packages
    private val isNightModeEnabled = true
    private val nightModeStartHour = 22
    private val nightModeEndHour = 6
    private val emergencyContact = "1234567890"
    private val allowedPackages = listOf("com.kyro", "com.android.dialer", "com.android.systemui", "com.whatsapp", "com.google.android.youtube")

    private enum class AutomationState { IDLE, WA_SEARCH_CONTACT, WA_TYPE_MESSAGE, YT_SEARCH, YT_TYPE, YT_SELECT, REELS_SCROLLING }
    private var currentState = AutomationState.IDLE
    private var pendingArg1: String? = null
    private var pendingArg2: String? = null

    private val scrollHandler = Handler(Looper.getMainLooper())
    private val reelsScrollRunnable = object : Runnable {
        override fun run() {
            if (currentState == AutomationState.REELS_SCROLLING) {
                performScrollUp()
                scrollHandler.postDelayed(this, 5000) // Scroll every 5 seconds
            }
        }
    }

    private val automationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.kyro.ACTION_AUTOMATION") {
                val actionType = intent.getStringExtra("action_type")
                val payload = intent.getStringExtra("payload") ?: ""

                when (actionType) {
                    "SCROLL_DOWN" -> {
                        performScrollDown()
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
                            pendingArg1 = parts[0]
                            pendingArg2 = parts[1]
                            currentState = AutomationState.WA_SEARCH_CONTACT
                            launchApp("com.whatsapp")
                        }
                    }
                    "YOUTUBE_AUTOMATION" -> {
                        pendingArg1 = payload
                        currentState = AutomationState.YT_SEARCH
                        launchApp("com.google.android.youtube")
                    }
                    "REELS_AUTOMATION" -> {
                        currentState = AutomationState.REELS_SCROLLING
                        scrollHandler.post(reelsScrollRunnable)
                    }
                    "STOP_AUTOMATION" -> {
                        currentState = AutomationState.IDLE
                        scrollHandler.removeCallbacksAndMessages(null)
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
            val packageName = event.packageName?.toString()

            // Feature 16: App Blocker
            if (isNightModeEnabled && isNightTime()) {
                if (packageName != null && !allowedPackages.contains(packageName)) {
                    Log.d("KYRO_ACC", "Blocking app: $packageName during Night Mode")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    return
                }
            }

            val root = rootInActiveWindow ?: return

            // Automation State Machine Dispatch
            if (currentState != AutomationState.IDLE) {
                if (packageName == "com.whatsapp" && (currentState == AutomationState.WA_SEARCH_CONTACT || currentState == AutomationState.WA_TYPE_MESSAGE)) {
                    handleWhatsAppAutomation(root)
                } else if (packageName == "com.google.android.youtube" && (currentState == AutomationState.YT_SEARCH || currentState == AutomationState.YT_TYPE || currentState == AutomationState.YT_SELECT)) {
                    handleYouTubeAutomation(root)
                }
            }
        } catch (e: Exception) {
            Log.e("KYRO_ACC", "Error processing accessibility event", e)
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

    // Feature 11: Auto Swipe Up for Reels
    private fun performScrollUp() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val middleX = displayMetrics.widthPixels / 2f
            val startY = displayMetrics.heightPixels * 0.2f
            val endY = displayMetrics.heightPixels * 0.8f

            val path = Path().apply {
                moveTo(middleX, startY)
                lineTo(middleX, endY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
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

    private fun launchApp(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("KYRO_ACC", "Failed to open $pkg", e)
            currentState = AutomationState.IDLE
        }
    }

    private fun handleWhatsAppAutomation(node: AccessibilityNodeInfo) {
        try {
            if (currentState == AutomationState.WA_SEARCH_CONTACT) {
                val searchNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/menuitem_search")
                if (searchNodes.isNotEmpty()) {
                    searchNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }

                val searchInputNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/search_src_text")
                if (searchInputNodes.isNotEmpty()) {
                    val arguments = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingArg1)
                    }
                    searchInputNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    Handler(Looper.getMainLooper()).postDelayed({
                        val refreshedRoot = rootInActiveWindow ?: return@postDelayed
                        val contactNodes = refreshedRoot.findAccessibilityNodeInfosByText(pendingArg1)
                        for (contactNode in contactNodes) {
                            if (contactNode.viewIdResourceName != "com.whatsapp:id/search_src_text") {
                                if (contactNode.isClickable) {
                                    contactNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    currentState = AutomationState.WA_TYPE_MESSAGE
                                    return@postDelayed
                                } else if (contactNode.parent?.isClickable == true) {
                                    contactNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    currentState = AutomationState.WA_TYPE_MESSAGE
                                    return@postDelayed
                                }
                            }
                        }
                    }, 1000)
                }
            } else if (currentState == AutomationState.WA_TYPE_MESSAGE) {
                val messageInputNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
                if (messageInputNodes.isNotEmpty()) {
                    val arguments = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingArg2)
                    }
                    messageInputNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    Handler(Looper.getMainLooper()).postDelayed({
                        val refreshedRoot = rootInActiveWindow ?: return@postDelayed
                        val sendNodes = refreshedRoot.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                        if (!sendNodes.isNullOrEmpty()) {
                            sendNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            currentState = AutomationState.IDLE
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                    }, 500)
                }
            }
        } catch (e: Exception) {
            Log.e("KYRO_ACC", "WA automation failed", e)
            currentState = AutomationState.IDLE
        }
    }

    // Feature 10: YouTube Search & Play Automation
    private fun handleYouTubeAutomation(node: AccessibilityNodeInfo) {
        try {
            if (currentState == AutomationState.YT_SEARCH) {
                val searchIcons = node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/menu_item_1") // Typical ID for search, varies heavily
                if (searchIcons.isNotEmpty()) {
                    searchIcons[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    currentState = AutomationState.YT_TYPE
                }
            } else if (currentState == AutomationState.YT_TYPE) {
                val searchInputs = node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/search_edit_text")
                if (searchInputs.isNotEmpty()) {
                    val arguments = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingArg1)
                    }
                    searchInputs[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    // Emulate 'Enter' keypress or tap on the first suggestion
                    Handler(Looper.getMainLooper()).postDelayed({
                        val refreshedRoot = rootInActiveWindow ?: return@postDelayed
                        val suggestions = refreshedRoot.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/search_query")
                        if (suggestions.isNotEmpty()) {
                            suggestions[0].parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            currentState = AutomationState.YT_SELECT
                        }
                    }, 1000)
                }
            } else if (currentState == AutomationState.YT_SELECT) {
                // Click the first video result in the list
                val videoList = node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/results")
                if (videoList.isNotEmpty() && videoList[0].childCount > 0) {
                    val firstVideo = videoList[0].getChild(0)
                    firstVideo?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    currentState = AutomationState.IDLE
                }
            }
        } catch (e: Exception) {
             Log.e("KYRO_ACC", "YT automation failed", e)
             currentState = AutomationState.IDLE
        }
    }

    override fun onInterrupt() {
        Log.e("KYRO_ACCESSIBILITY", "Service Interrupted")
        currentState = AutomationState.IDLE
    }

    // Feature 15: Silent SOS Mode (Hardware Buttons)
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
            val locationUrl = "http://maps.google.com/?q=0,0" // Placeholder for real GPS
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
        scrollHandler.removeCallbacksAndMessages(null)
    }
}
