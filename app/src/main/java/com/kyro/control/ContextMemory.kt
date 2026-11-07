package com.kyro.control

class ContextMemory {

    private var lastSubject: String? = null
    private var lastAction: String? = null

    fun updateContext(text: String, extractedEntity: String?) {
        if (extractedEntity != null) {
            lastSubject = extractedEntity
        }
    }

    fun resolvePronouns(text: String): String {
        val pronouns = listOf("unko", "usko", "isko", "him", "her", "them", "it")
        var resolvedText = text.lowercase()

        if (lastSubject != null) {
            for (pronoun in pronouns) {
                if (resolvedText.contains(pronoun)) {
                    resolvedText = resolvedText.replace(pronoun, lastSubject!!)
                }
            }
        }
        return resolvedText
    }

    fun clear() {
        lastSubject = null
        lastAction = null
    }
}
