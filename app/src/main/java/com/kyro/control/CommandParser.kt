package com.kyro.control

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kyro.brain.RawBrainQuery
import com.kyro.brain.TermuxServerClient
import com.kyro.service.KyroAccessibilityService

class CommandParser(
    private val context: Context,
    private val brainQuery: RawBrainQuery,
    private val termuxClient: TermuxServerClient,
    private val contextMemory: ContextMemory
) {

    private val stopWords = listOf("zara", "jaldi", "bhai", "yaar", "please", "kyro")

    suspend fun processCommand(rawText: String): CommandResponse {
        var cleanedText = rawText.lowercase()
        stopWords.forEach { word ->
            cleanedText = cleanedText.replace("\\b$word\\b".toRegex(), "").trim()
        }

        val resolvedText = contextMemory.resolvePronouns(cleanedText)

        // Upgrade 4: Smart Macros (Teach Mode)
        val teachRegex = Regex("naya command seekho (.+) ke liye (.+)")
        val teachMatch = teachRegex.find(resolvedText)
        if (teachMatch != null) {
            val trigger = teachMatch.groupValues[1].trim()
            val actions = teachMatch.groupValues[2].trim()
            brainQuery.learnCommand(trigger, actions)
            return CommandResponse("LEARNED", responseText = "Seekh liya sir. $trigger pe ab mai $actions karunga.")
        }

        // Special Hardcoded Triggers
        if (resolvedText.contains("good night")) {
            return CommandResponse(action = "SLEEP_MODE", responseText = "Good night Sir.")
        }

        // Scroll / Click / Read Screen dynamic accessibility matching
        val clickRegex = Regex("click on (.+)")
        val clickMatch = clickRegex.find(resolvedText)
        if (clickMatch != null) {
            val textToClick = clickMatch.groupValues[1].trim()
            triggerAccessibilityAction("CLICK_TEXT", textToClick)
            return CommandResponse("ACCESSIBILITY", responseText = "Clicking $textToClick")
        }

        // Try Tier 1: Local DB
        val brainResult = brainQuery.findCommand(resolvedText)
        if (brainResult != null) {
            return executeTier1Command(resolvedText, brainResult.action, brainResult.parameters)
        }

        // Fallback to Tier 2: Termux AI
        val aiResponse = termuxClient.sendQuery(resolvedText)
        return CommandResponse("SPEAK", responseText = aiResponse)
    }

    private suspend fun executeTier1Command(originalText: String, action: String, parameters: String): CommandResponse {
        when (action) {
            "CALL_CONTACT" -> {
                val entity = originalText.replace("call", "").trim()
                contextMemory.updateContext(originalText, entity)
                return CommandResponse(action, entity, "Calling $entity")
            }
            "MACRO" -> {
                // Execute chained commands sequentially by splitting on " aur " (and) or ","
                val chainedActions = parameters.split(Regex(" aur |,")).map { it.trim() }
                for (chainedAction in chainedActions) {
                    val subResult = brainQuery.findCommand(chainedAction)
                    if (subResult != null) {
                         executeTier1Command(chainedAction, subResult.action, subResult.parameters)
                    }
                }
                return CommandResponse("EXECUTE_MACRO", parameters, "Done.")
            }
            "WHATSAPP_MSG" -> {
                val regex = Regex("(.+) ko whatsapp par message bhejo (.+)")
                val match = regex.find(originalText)
                if (match != null) {
                    val contact = match.groupValues[1].trim()
                    val msg = match.groupValues[2].trim()
                    triggerAccessibilityAction("WHATSAPP_AUTOMATION", "$contact|$msg")
                    return CommandResponse("WHATSAPP_MSG", responseText = "Sending message to $contact.")
                }
            }
            "ACCESSIBILITY_ACTION" -> {
                triggerAccessibilityAction(parameters, "")
                return CommandResponse("ACCESSIBILITY", responseText = "Done.")
            }
        }
        return CommandResponse(action, parameters, "Done.")
    }

    private fun triggerAccessibilityAction(action: String, payload: String) {
        val intent = Intent("com.kyro.ACTION_AUTOMATION")
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
