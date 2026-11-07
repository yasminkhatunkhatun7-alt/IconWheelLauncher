package com.kyro.control

import com.kyro.brain.RawBrainQuery
import com.kyro.brain.TermuxServerClient

class CommandParser(
    private val brainQuery: RawBrainQuery,
    private val termuxClient: TermuxServerClient,
    private val contextMemory: ContextMemory
) {

    private val stopWords = listOf("zara", "jaldi", "bhai", "yaar", "please")

    suspend fun processCommand(rawText: String): CommandResponse {
        // 1. Clean the input
        var cleanedText = rawText.lowercase()
        stopWords.forEach { word ->
            cleanedText = cleanedText.replace("\\b$word\\b".toRegex(), "").trim()
        }

        // 2. Resolve pronouns based on context
        val resolvedText = contextMemory.resolvePronouns(cleanedText)

        // 3. Special Hardcoded Triggers
        if (resolvedText.contains("good night")) {
            return CommandResponse(action = "SLEEP_MODE", responseText = "Good night Sir.")
        }

        // 4. Try Tier 1: Local DB
        val brainResult = brainQuery.findCommand(resolvedText)
        if (brainResult != null) {
            // Update context if it's a call command (dummy logic for entity extraction)
            if (brainResult.action == "CALL_CONTACT") {
                val entity = resolvedText.replace("call", "").trim()
                contextMemory.updateContext(resolvedText, entity)
                return CommandResponse(
                    action = brainResult.action,
                    payload = entity,
                    responseText = "Calling $entity"
                )
            }

            return CommandResponse(
                action = brainResult.action,
                payload = brainResult.parameters,
                responseText = "Done."
            )
        }

        // 5. Fallback to Tier 2: Termux AI
        val aiResponse = termuxClient.sendQuery(resolvedText)
        return CommandResponse(action = "SPEAK", responseText = aiResponse)
    }
}

data class CommandResponse(
    val action: String,
    val payload: String = "",
    val responseText: String = ""
)
