package com.kyro.control

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import com.kyro.brain.RawBrainQuery
import com.kyro.brain.AiBackend
import com.kyro.brain.TermuxServerClient
import java.util.Calendar
import android.app.NotificationManager

class CommandParser(
    private val context: Context,
    private val brainQuery: RawBrainQuery,
    private val aiBackend: AiBackend,
    private val contextMemory: ContextMemory
) {

    private val stopWords = listOf("zara", "jaldi", "bhai", "yaar", "please", "kyro")
    private val securityPassword = "obsidian"
    private var isLocked = true
    private var pendingLockedCommand: String? = null
    private var pendingFuzzyAction: String? = null

    suspend fun processCommand(rawText: String): List<CommandResponse> {
        var cleanedText = rawText.lowercase()
        stopWords.forEach { word ->
            cleanedText = cleanedText.replace("\\b$word\\b".toRegex(), "").trim()
        }

        val resolvedText = contextMemory.resolvePronouns(cleanedText)

        if (isLocked && pendingLockedCommand != null) {
            if (resolvedText.contains(securityPassword)) {
                isLocked = false
                val cmdToRun = pendingLockedCommand!!
                pendingLockedCommand = null
                return processMultiActions(cmdToRun)
            } else {
                pendingLockedCommand = null
                return listOf(CommandResponse("DENIED", responseText = "Incorrect password. Access denied."))
            }
        }

        // Step 2: Fuzzy         val sensitiveKeywords = listOf("call", "message", "sms", "pay") Broken Word Intelligence
        if (pendingFuzzyAction != null) {
            val completeAction = "$pendingFuzzyAction $resolvedText"
            pendingFuzzyAction = null
            return processMultiActions(completeAction)
        }

        if (resolvedText == "kholo" || resolvedText == "open" || resolvedText == "chalao") {
            pendingFuzzyAction = resolvedText
            return listOf(CommandResponse("ASK_CLARIFICATION", responseText = "Sir, aap kya kholna chahte hain?"))
        }

        val sensitiveKeywords = listOf("call", "message", "sms", "pay")
        if (isLocked && sensitiveKeywords.any { resolvedText.contains(it) }) {
            pendingLockedCommand = resolvedText
            return listOf(CommandResponse("LOCK", responseText = "Password bolo."))
        }

        return processMultiActions(resolvedText)
    }

    private suspend fun processMultiActions(resolvedText: String): List<CommandResponse> {
        val multiActions = resolvedText.split(Regex("\\s+(aur|and)\\s+"))
        val responses = mutableListOf<CommandResponse>()
        for (action in multiActions) {
             val response = processSingleCommand(action.trim())
             responses.addAll(response)
        }
        return responses
    }

    private suspend fun processSingleCommand(resolvedText: String): List<CommandResponse> {
        val teachRegex = Regex("naya command seekho (.+) ke liye (.+)")
        val teachMatch = teachRegex.find(resolvedText)
        if (teachMatch != null) {
            val trigger = teachMatch.groupValues[1].trim()
            val actions = teachMatch.groupValues[2].trim()
            brainQuery.learnCommand(trigger, actions)
            return listOf(CommandResponse("LEARNED", responseText = "Seekh liya sir. $trigger pe ab mai $actions karunga."))
        }

        if (resolvedText.contains("good night")) {
            triggerSleepMode()
            return listOf(CommandResponse(action = "SLEEP_MODE", responseText = "Good night Sir."))
        }

        if (resolvedText.contains("morning briefing") || resolvedText.contains("daily briefing")) {
            val briefing = generateDailyBriefing()
            return listOf(CommandResponse(action = "BRIEFING", responseText = briefing))
        }

        val clickRegex = Regex("click on (.+)")
        val clickMatch = clickRegex.find(resolvedText)
        if (clickMatch != null) {
            val textToClick = clickMatch.groupValues[1].trim()
            triggerAccessibilityAction("CLICK_TEXT", textToClick)
            return listOf(CommandResponse("ACCESSIBILITY", responseText = "Clicking $textToClick"))
        }

        val brainResult = brainQuery.findCommand(resolvedText)
        if (brainResult != null) {
            return executeTier1Command(resolvedText, brainResult.action, brainResult.parameters)
        }

        val pastContext = brainQuery.getRelevantContext(resolvedText)
        val aiResponse = aiBackend.sendQuery(resolvedText, pastContext)
        brainQuery.saveConversation(resolvedText, aiResponse)
        return listOf(CommandResponse("SPEAK", responseText = aiResponse))
    }

    private suspend fun executeTier1Command(originalText: String, action: String, parameters: String): List<CommandResponse> {
        when (action) {
            "CALL_CONTACT" -> {
                val entity = originalText.replace("call", "").trim()
                contextMemory.updateContext(originalText, entity)
                return listOf(CommandResponse(action, entity, "Calling $entity"))
            }
            "MACRO" -> {
                return processMultiActions(parameters)
            }
            "WHATSAPP_MSG" -> {
                val regex = Regex("(.+) ko whatsapp par message bhejo (.+)")
                val match = regex.find(originalText)
                if (match != null) {
                    val contact = match.groupValues[1].trim()
                    val msg = match.groupValues[2].trim()
                    triggerAccessibilityAction("WHATSAPP_AUTOMATION", "$contact|$msg")
                    return listOf(CommandResponse("WHATSAPP_MSG", responseText = "Sending message to $contact."))
                }
            }
            "YOUTUBE_SEARCH" -> {
                val entity = originalText.replace("youtube par", "").replace("chalao", "").replace("play", "").trim()
                triggerAccessibilityAction("YOUTUBE_AUTOMATION", entity)
                return listOf(CommandResponse("YOUTUBE", responseText = "Playing $entity on YouTube."))
            }
            "FAKE_CALL" -> {
                val intent = Intent(context, com.kyro.ui.FakeCallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return listOf(CommandResponse("FAKE_CALL", responseText = "Generating call."))
            }
            "REELS_SCROLL" -> {
                triggerAccessibilityAction("REELS_AUTOMATION", "")
                return listOf(CommandResponse("REELS", responseText = "Auto scrolling activated."))
            }
            "DEEP_LINK" -> {
                triggerAccessibilityAction("DEEP_LINK_CLICK", parameters)
                return listOf(CommandResponse("DEEP_LINK", responseText = "Trying to open $parameters."))
            }
            "ACCESSIBILITY_ACTION" -> {
                triggerAccessibilityAction(parameters, "")
                return listOf(CommandResponse("ACCESSIBILITY", responseText = "Done."))
            }
        }
        return listOf(CommandResponse(action, parameters, "Done."))
    }

    private fun triggerSleepMode() {
        try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 1)

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        } catch (e: Exception) {
            Log.e("KYRO_CMD", "Failed to trigger sleep mode fully due to permissions", e)
        }
    }

    private fun generateDailyBriefing(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        val timeString = "$hour $min"

        val batteryPct = 85
        val screenTime = "2 hours 15 minutes"

        return "Good morning sir. The time is $timeString. Battery is at $batteryPct percent. Your screen time today is $screenTime. Have a great day ahead."
    }

    private fun triggerAccessibilityAction(action: String, payload: String) {
        val intent = Intent("com.kyro.ACTION_AUTOMATION")
        intent.setPackage(context.packageName)
        intent.putExtra("action_type", action)
        intent.putExtra("payload", payload)
        context.sendBroadcast(intent)
    }
}

data class CommandResponse(
    val action: String,
    val payload: String = "",
    val responseText: String = ""
)
